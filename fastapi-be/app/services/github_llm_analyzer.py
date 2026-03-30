import os
import asyncio
import json
from langchain.chat_models import init_chat_model
from langchain_core.prompts import PromptTemplate
from langchain_core.output_parsers import JsonOutputParser
from pydantic import BaseModel, Field
import time

# 기존의 github_data_test.py 에서 만들어뒀던 마크다운 수집 함수를 그대로 불러옵니다.
from github_data_test import main as get_github_context

# ==========================================
# ==========================================
os.environ["OPENAI_API_KEY"] = "S14P22A506-20649484-f08a-4522-a9fe-a26f5b4a9246"

# 중요! langchain의 BASE_URL을 GMS로 설정합니다. 
os.environ["OPENAI_API_BASE"] = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"

# 1. Pydantic을 이용해 LLM이 '반드시' 지켜야 할 출력 형태(Schema)를 정의합니다.
class ActivitySummary(BaseModel):
    id: int = Field(description="작업의 고유 ID")
    tech_stacks: list[str] = Field(description="어떤 구체적 기술을 썼는지 1~3개 추출. 'NPM 패키지', 'Python 패키지' 같은 단어는 금지! (예: React, TypeScript, socket.io, Python)")
    summary: str = Field(description="수정된 파일명(changed_files)과 커밋 메시지를 참고하여, '무슨 기술적 구현/변경'을 했는지 시니어 개발자 관점으로 요약. 커밋 메시지를 그대로 베끼지 말 것!")
    category: str = Field(description="개발(실제 프로젝트 구현), 학습(알고리즘, TIL 등), 취준(면접, 코딩테스트 등), 기타 중 하나로 분류")

def analyze_github_activities(github_markdown_context: str) -> list[dict]:
    """메모리 상의 마크다운 텍스트를 받아서 GPT-4o-mini로 분석한 뒤 JSON 리스트로 리턴합니다."""
    
    # 2. 모델 설정 (비용이 가장 저렴하고 똑똑한 gpt-4o-mini)
    # SSAFY GMS 커스텀 엔드포인트를 사용하기 위해 init_chat_model과 환경변수를 사용합니다.
    llm = init_chat_model("gpt-4o-mini", model_provider="openai", temperature=0.1)

    # 3. Pydantic 모델을 기반으로 JsonOutputParser 생성
    parser = JsonOutputParser(pydantic_object=ActivitySummary)

    # 4. 프롬프트 작성
    # {format_instructions} 부분에 LangChain이 "어떤 JSON 구조로 답해라"는 설명을 자동으로 주입합니다.
    prompt = PromptTemplate(
        template="""너는 시니어 개발자 프로필 분석 AI '카이로스'야.
아래의 [활동 내역]은 유저가 깃허브 여러 레포지토리에서 작업한 커밋과 PR 정보야.
각 레포의 사용 기술과, 커밋에서 실제로 수정된 파일 경로(changed_files)를 면밀히 분석해서 아래 규칙을 무조건 지켜서 JSON을 생성해.

<규칙>
1. tech_stacks: 'NPM 패키지', 'Python 패키지' 같은 패키지 매니저 이름은 무조건 빼라. 의존성 목록이나 수정된 파일의 확장자(.tsx -> TypeScript, React)를 보고 실제 뼈대가 되는 기술명만 추출하라.
2. summary: 커밋 메시지를 앵무새처럼 반복하지 마라! 수정된 파일을 어떻게 만졌는지(changed_files)를 유추해서 "어떤 기술적 구현/변경"을 했는지 구조적으로 요약해라. 
   - [나쁜 예시]: "게임 시간 조정", "README 수정 및 게임 로직 변경"
   - [좋은 예시]: "TypeScript 상수로 게임 타이머 전역 상태 시간값 변경", "zustand 스토어를 활용해 후원자B 게임 아이템 중첩 방지 로직 보완"

{format_instructions}

[활동 내역(컨텍스트)]
{github_context}
""",
        input_variables=["github_context"],
        partial_variables={"format_instructions": parser.get_format_instructions()},
    )
    
    # 5. LangChain 구조 결합 (프롬프트 -> 모델 -> 파서)
    chain = prompt | llm | parser
    
    print("\n🤖 LLM 분석 요청 중 (gpt-4o-mini)...")
    # 텍스트를 던지면, 찰떡같이 분석해서 파이썬 리스트(List of Dict) 형식으로 리턴해줍니다.
    result_json = chain.invoke({"github_context": github_markdown_context})
    
    return result_json

async def run_analysis():
    print("▶️ 1. 깃허브 데이터 수집 시작... (github_data_test.py 모듈 활용)")
    
    # 파일 입출력(디스크) 없이 메모리 상에서 바로 긴 마크다운 문자열을 받아옵니다.
    context = await get_github_context()
    
    if not context:
        print("❌ 수집된 데이터가 없습니다.")
        return
        
    print(f"\n▶️ 2. 데이터 수집 완료! (총 텍스트 길이: {len(context)}) - LLM으로 넘깁니다.")
    
    # 메모리 상의 문자열을 곧바로 분석기 함수로 넘깁니다.
    start_time = time.time()
    results = analyze_github_activities(context)
    
    print("\n✅ LLM 분석 완료! (완벽한 파이썬 JSON 리스트 형태입니다.)")
    print("=" * 60)
    # 파이썬 딕셔너리를 예쁘게 출력
    print(json.dumps(results, indent=2, ensure_ascii=False))
    print("=" * 60)
    end_time = time.time()
    print(f"⏱️ LLM 데이터전처리 소요 시간: {end_time - start_time:.2f}초")
    # 여기서 results 변수를 바로 FastAPI의 return 값으로 쓰거나 DB에 저장하시면 됩니다!

if __name__ == "__main__":
    import sys
    if sys.platform == 'win32':
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    
    # 실행!
    asyncio.run(run_analysis())
