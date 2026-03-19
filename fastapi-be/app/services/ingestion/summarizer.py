import os
import asyncio
from langchain.chat_models import init_chat_model
from langchain_core.prompts import PromptTemplate

# API Key 설정 (profile_analyzer에서 사용하는 것과 동일하게 임시 설정)
if "OPENAI_API_KEY" not in os.environ:
    os.environ["OPENAI_API_KEY"] = "S14P22A506-20649484-f08a-4522-a9fe-a26f5b4a9246"
    os.environ["OPENAI_API_BASE"] = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"

def summarize_article_sync(title: str, text: str) -> str:
    """단일 아티클의 제목과 본문을 받아 3줄 이내로 요약합니다."""
    if not text:
        return ""
        
    llm = init_chat_model("gpt-4o-mini", model_provider="openai", temperature=0.1)
    prompt = PromptTemplate(
        template="""당신은 시니어 개발자입니다. 다음 기술 블로그 글을 읽고, 주니어 개발자에게 이 글을 추천하는 이유를 설명하듯이 3줄 이내로 핵심만 요약해 줘.

[제목]: {title}
[본문 일부]:
{text}
""",
        input_variables=["title", "text"]
    )
    chain = prompt | llm
    
    # 텍스트가 너무 길면 앞부분 4000자만 잘라서 넘김 (비용 및 토큰 제한 방지)
    res = chain.invoke({"title": title, "text": text[:4000]})
    # 문자열로 안전하게 형변환하여 반환
    return str(res.content) if hasattr(res, 'content') else str(res)

async def summarize_articles(articles: list[dict]) -> list[dict]:
    """크롤링된 문서 목록을 받아 각각 다량의 LLM 요약을 병렬로 처리합니다."""
    print(f"  [Summarizer] {len(articles)}개 문서 3줄 요약 시작 (gpt-4o-mini 병렬 처리)...")
    
    # SSAFY proxy API의 Rate Limit (429 Too Many Requests 등) 방지를 위해 동시성 제한
    sem = asyncio.Semaphore(5)
    
    async def process(article):
        # 만약 본문이 없다면 빈 요약 삽입
        if not article.get("raw_text"):
            article["summary"] = ""
            return article
            
        async with sem:
            try:
                summary = await asyncio.to_thread(
                    summarize_article_sync, 
                    article.get("title", ""), 
                    article["raw_text"]
                )
                article["summary"] = summary
                print(f"    - 요약 완료: {article.get('title', '')[:30]}...")
            except Exception as e:
                print(f"    - 요약 실패 (API 에러): {article.get('title', '')[:30]}... ({e})")
                article["summary"] = "" # 실패 시 빈 문자열
                
        return article

    # 네트워크 IO 바운드 작업이므로 gather로 한꺼번에 던짐
    tasks = [process(a) for a in articles]
    results = await asyncio.gather(*tasks)
    return list(results)
