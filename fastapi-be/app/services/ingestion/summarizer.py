import os
import asyncio
from langchain.chat_models import init_chat_model
from langchain_core.prompts import PromptTemplate

# API Key 설정 (profile_analyzer에서 사용하는 것과 동일하게 임시 설정)
if "OPENAI_API_KEY" not in os.environ:
    os.environ["OPENAI_API_KEY"] = "S14P22A506-20649484-f08a-4522-a9fe-a26f5b4a9246"
    os.environ["OPENAI_API_BASE"] = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"

import json
import re

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


def summarize_and_extract_skills_sync(title: str, text: str) -> dict:
    """단일 아티클의 제목과 본문을 받아 3줄 요약과 핵심 기술 스택 리스트를 반환합니다."""
    if not text:
        return {"summary": "", "skills": []}
        
    llm = init_chat_model("gpt-4o-mini", model_provider="openai", temperature=0.1)
    prompt = PromptTemplate(
        template="""당신은 시니어 개발자입니다. 다음 기술 블로그 글을 읽고 아래의 두 가지 정보를 추출하여 반드시 순수 JSON 형식으로만 반환해 주세요. (마크다운 코드블록 등 다른 설명은 일절 포함하지 마세요)

1. "summary": 주니어 개발자에게 이 글을 추천하는 이유를 설명하듯이 3줄 이내로 핵심만 요약
2. "skills": 본문에서 주로 다루는 구체적인 기술 스택, 도구, 또는 핵심 아키텍처/트렌드 키워드 (예: React, Spring Boot, Kubernetes, LLM, RAG, Kafka, MSA 등). 
   - 'Frontend', 'Backend', '기획' 처럼 너무 광범위한 직무 카테고리 단어는 제외하되, 'LLM', 'RAG', 'Prompt Engineering' 등 구체적인 기술/개념 키워드는 적극 포함하세요.
   - 단편적으로 언급된 언어(Python, Java 등)보다는 글의 핵심 주제가 되는 기술명 위주로 최대 5개 추출하세요.
   - 관련 기술이 없으면 빈 리스트를 반환하세요.

[JSON 반환 예시]
{{
  "summary": "안전하게 Nginx에서 API Gateway로 마이그레이션하는 방법을 다룹니다...",
  "skills": ["Nginx", "API Gateway", "Spring Cloud"]
}}

[제목]: {title}
[본문 일부]:
{text}
""",
        input_variables=["title", "text"]
    )
    chain = prompt | llm
    
    # 텍스트가 너무 길면 앞부분 4000자만 잘라서 넘김 (요약 및 스택 파악엔 보통 충분함)
    res = chain.invoke({"title": title, "text": text[:4000]})
    raw_response = str(res.content) if hasattr(res, 'content') else str(res)
    
    # 결과가 마크다운 코드블록으로 감싸져 있을 수 있으므로 정규식으로 JSON 부분만 추출
    json_str_match = re.search(r'\{[\s\S]*\}', raw_response)
    if json_str_match:
        try:
            parsed = json.loads(json_str_match.group(0))
            return {
                "summary": parsed.get("summary", ""),
                "skills": parsed.get("skills", [])
            }
        except json.JSONDecodeError:
            pass
            
    return {"summary": raw_response, "skills": []}

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
                # 토스처럼 구체적인 태그 수집이 어려운 곳만 스킬 추출 로직을 사용합니다.
                if article.get("source_type") == "tech_blog_toss":
                    result = await asyncio.to_thread(
                        summarize_and_extract_skills_sync, 
                        article.get("title", ""), 
                        article["raw_text"]
                    )
                    article["summary"] = result.get("summary", "")
                    
                    extracted_skills = [s.lower() for s in result.get("skills", [])]
                    existing_skills = article.get("skill", [])
                    article["skill"] = list(dict.fromkeys(existing_skills + extracted_skills))
                    
                    print(f"    - 파싱 완료: {article.get('title', '')[:30]}... | 추출 스택: {extracted_skills}")
                else:
                    # 카카오, 우아한형제들 등은 원본 3줄 요약만 수행합니다.
                    summary = await asyncio.to_thread(
                        summarize_article_sync, 
                        article.get("title", ""), 
                        article["raw_text"]
                    )
                    article["summary"] = summary
                    print(f"    - 요약 완료: {article.get('title', '')[:30]}...")
            except Exception as e:
                print(f"    - 파싱 실패 (API 또는 파싱 에러): {article.get('title', '')[:30]}... ({e})")
                article["summary"] = "" # 실패 시 빈 문자열
                
        return article

    # 네트워크 IO 바운드 작업이므로 gather로 한꺼번에 던짐
    tasks = [process(a) for a in articles]
    results = await asyncio.gather(*tasks)
    return list(results)
