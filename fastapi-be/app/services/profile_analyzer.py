import os
import asyncio
import time
import json
from langchain.chat_models import init_chat_model
from langchain_core.prompts import PromptTemplate
from langchain_core.output_parsers import JsonOutputParser
from pydantic import BaseModel, Field

# 1. 분리해둔 데이터 수집 모듈들 불러오기
from .github_data_preprocess import main as get_github_context
from .velog_data_preprocess import main as get_velog_context


from dotenv import load_dotenv

# ==========================================
# 환경 변수 로드 (API Key 등)
load_dotenv()

# 2. 통합된 분석 결과를 받을 Pydantic 스키마 정의
class ActivitySummary(BaseModel):
    id: int = Field(description="작업의 고유 ID (Github은 1부터, Velog 포스트는 이어서 부여된 번호)")
    type: str = Field(description="이 활동의 출처: 'Github Commit', 'Github PR', 'Velog Post' 중 1개 선택")
    tech_stacks: list[str] = Field(description="어떤 구체적 기술을 썼는지 1~3개 추출. 'NPM 패키지', 'Python 패키지' 금지! (예: React, TypeScript, Python 등)")
    summary: str = Field(description="어떤 기술적 구현/학습/트러블슈팅을 했는지 시니어 관점으로 2줄 내로 요약. 커밋 메시지 앵무새 반복 금지!")
    category: str = Field(description="개발(실제 프로젝트 구현), 학습(알고리즘, 블로그 포스팅, TIL 등), 취준(면접, 코딩테스트,자소서 등), 기타 중 하나로 분류")

def analyze_all_activities(merged_context: str) -> list[dict]:
    """Github과 Velog 데이터가 뷰티풀하게 합쳐진 마크다운을 LLM에 한 번에 던져 분석합니다."""
    
    # 3. 모델 및 커스텀 파서 설정
    llm = init_chat_model("gpt-4o-mini", model_provider="openai", temperature=0.1)
    parser = JsonOutputParser(pydantic_object=ActivitySummary)

    # 4. 프롬프트 템플릿
    prompt = PromptTemplate(
        template="""너는 시니어 개발자 프로필 분석 AI '카이로스'야.
아래의 [통합 활동 내역]은 유저가 깃허브에서 작업한 "코드 구현 내역(Commit/PR)"과 벨로그에 작성한 "기술 블로그 포스팅(Velog)"이 합쳐져 있어.
각 출처의 데이터 구조(마크다운과 CSV)를 면밀히 분석해서 아래 규칙을 무조건 지켜서 JSON을 생성해.

<규칙>
1. tech_stacks: 'NPM 패키지', 'Python 패키지' 같은 패키지 매니저 이름은 무조건 빼라. 의존성 목록이나 수정된 파일(.tsx), 블로그 태그를 보고 뼈대가 되는 기술명만 추출하라.
2. summary: 커밋 메시지나 블로그 제목을 그대로 반복하지 마라.
   - [Github 경우]: 수정된 파일(changed_files)과 겹쳐봐서 "어떤 기술적 구현/구조 변경"을 했는지 파악.
   - [Velog 경우]: 본문 요약(content_snippet)을 보고 "어떤 기술적 난제를 해결했거나 무엇을 깊게 학습했는지" 파악.
3. category: 활동의 본질을 파악하여 '개발', '학습', '취준', '기타' 중 하나로 정확하게 분류해라.
4. type: Github 데이터면 'Github Commit'이나 'Github PR'로 적고, Velog 데이터면 'Velog Post'로 기재.

{format_instructions}

[통합 활동 내역(컨텍스트)]
{merged_context}
""",
        input_variables=["merged_context"],
        partial_variables={"format_instructions": parser.get_format_instructions()},
    )
    
    # 5. 실행
    chain = prompt | llm | parser
    return chain.invoke({"merged_context": merged_context})

def generate_final_profile(activities_json: list[dict]) -> dict:
    """모든 개별 분석이 끝난 JSON 데이터를 모아 최종 1장짜리 요약 프로필을 만듭니다."""
    
    llm = init_chat_model("gpt-4o-mini", model_provider="openai", temperature=0.1)
    
    class FinalProfile(BaseModel):
        headline: str = Field(description="이 개발자를 한 줄로 표현하는 매력적인 캐치프레이즈")
        skill_frequency: dict[str, int] = Field(description="원본 데이터를 집계하여, 기술별로 총 몇 번 커밋/태그가 등장했는지 카운트. 높은 순 정렬. 예: {'React': 23, 'TypeScript': 18}")
        # recommended_positions: dict[str, str] = Field(description="단순 '프론트/백엔드'가 아닌, 기술 스택과 도메인을 분석하여 도출한 구체적인 산업군/직무. (예: {'핀테크 프론트엔드 엔지니어': '적합도 높음', '웹 러닝 플랫폼 풀스택': '가능성 있음'})")
        # core_competencies: list[str] = Field(description="가장 많이 사용했고 자신있는 핵심 기술 스택 3~5개")
        experience_summary: str = Field(description="어떤 도메인(게임, 백엔드 등)에서 어떤 기술적 경험을 쌓아왔는지 3~4줄로 요약")
        learning_attitude: str = Field(description="Velog나 TIL 학습 기록을 바탕으로 이 개발자의 성장 가능성과 태도 요약")
        possible_positions: list[str] = Field(description="이 개발자가 기여할 수 있는 상세 직무 3~5개")
        
    parser = JsonOutputParser(pydantic_object=FinalProfile)
    
    prompt = PromptTemplate(
        template="""너는 기업의 시니어 테크 리크루터야.
아래는 한 개발자의 커밋과 블로그 포스팅을 분석한 전체 활동 내역(JSON 배열)이야.
이 데이터를 바탕으로 프론트엔드 대시보드 화면에 바로 뿌려줄 '현재 학습 상태 분석 결과' JSON을 만들어줘.

<필수 지침>
1. skill_frequency: 원본 통계를 바탕으로 실제로 가장 많이 나온 기술의 빈도수를 세어서 내림차순으로 매핑할 것.
2. possible_positions: **단순히 '프론트엔드 개발자', '백엔드 개발자' 같은 뻔하고 쓸모없는 단어 절대 금지!** 
   - [나쁜 예시]: 프론트엔드 개발자, AI 개발자
   - [좋은 예시]: 핀테크/금융권 실시간 시스템 엔지니어, EdTech 인터랙티브 웹 프론트엔드, B2B SaaS 데이터 플랫폼 백엔드
   - 기술 스택(소켓통신이면 실시간/게임, 보안이면 금융권, 데이터 다루면 SaaS 등)의 맥락을 분석해서 아주 구체적인 산업군과 엮은 직무를 2개 추천해라.

{format_instructions}

[전체 활동 데이터]
{activities}
""",
        input_variables=["activities"],
        partial_variables={"format_instructions": parser.get_format_instructions()},
    )
    
    chain = prompt | llm | parser
    print("\n🧠 최종 대시보드 맞춤형 프로필 생성 중 (gpt-4o-mini)...")
    return chain.invoke({"activities": json.dumps(activities_json, ensure_ascii=False)})

async def regenerate_profile_with_feedback(previous_profile: dict, user_feedback: str) -> dict:
    """사용자가 1페이지 요약본을 보고 '아니오, 수정할게요'를 클릭했을 때, 대량의 데이터 재처리 없이 직전 분석 결과(JSON) 객체만으로 즉시 교정하는 특고속 함수"""
    
    llm = init_chat_model("gpt-4o-mini", model_provider="openai", temperature=0.1)
    
    class FinalProfile(BaseModel):
        # recent_skills: list[str] = Field(description="이전 분석 결과 또는 사용자의 피드백을 반영하여 수정된 핵심 기술 4~5개")
        skill_frequency: dict[str, int] = Field(description="이전 결과의 빈도수를 그대로 유지할 것. (피드백에서 명시적으로 특정 기술을 지워달라고 한 경우에만 제거)")
        recommended_positions: dict[str, str] = Field(description="유저 피드백의 의도를 직접적으로 수용하여 새롭게 도출한 뾰족한 추천 직무 2개")
        summary: str = Field(description="사용자의 기술을 서술형으로 요약하되, 유저 피드백의 수정/추가 요청 의도를 가장 강력하게 반영할 것.")

    parser = JsonOutputParser(pydantic_object=FinalProfile)
    
    prompt = PromptTemplate(
        template="""너는 기업의 시니어 테크 리크루터야.
아래는 네가 방금 전에 작성한 1페이지 프로필 분석 결과(JSON) 객체야.

[이전 분석 결과]
{previous_profile}

[사용자의 직접 교정/피드백 요청 사항]
🚨 "{user_feedback}" 🚨

위의 사용자의 <요청 사항>을 [절대적 최우선]으로 반영하여 기존 JSON을 수정해서 다시 출력해 줘!!! 
사용자가 원치 않거나 자신없어하는 직무나 기술이 있다면 recommended_positions나 summary에서 과감히 반영하여 고쳐 쓰고, 반대로 강조하고 싶은 기술이 있다면 summary에 강력하게 어필해서 문장을 다시 작성해. 

<주의사항>
- skill_frequency(통계)는 피드백에서 "이 언어 빼주세요" 처럼 명시적인 제외 요청이 없는 한 원본 숫자(이전 분석 결과)를 그대로 똑같이 유지해라!!!

{format_instructions}
""",
        input_variables=["previous_profile", "user_feedback"],
        partial_variables={"format_instructions": parser.get_format_instructions()},
    )
    
    chain = prompt | llm | parser
    print("\n🧠 [유저 피드백 반영] 이전 프로필 재수정 중 (gpt-4o-mini)...")
    return await asyncio.to_thread(
        chain.invoke, 
        {"previous_profile": json.dumps(previous_profile, ensure_ascii=False), "user_feedback": user_feedback}
    )


async def start_github_collection(github_token: str, github_username: str):
    print("▶️ 1-1. GitHub 데이터만 사전 수집 시작... ⚡")
    collection_start = time.time()
    
    github_context = await get_github_context(github_token, github_username)
    
    collection_end = time.time()
    print(f"\n▶️ 1-1. GitHub 별도 수집 완벽히 종료! (총 소요 시간: {collection_end - collection_start:.2f}초)")
    return github_context


async def start_velog_and_analysis(velog_username: str, github_context: str):
    print("▶️ 1-2. Velog 수집 및 통합 분석 대기... ⚡")
    collection_start = time.time()
    
    velog_context = await get_velog_context(velog_username)
    
    collection_end = time.time()
    print(f"\n▶️ 1-2. Velog 수집 종료! (소요 시간: {collection_end - collection_start:.2f}초)")
    
    github_context = github_context or "Github 활동 내역 없음"
    velog_context = velog_context or "Velog 활동 내역 없음"
    
    # Github 컨텍스트가 너무 길면 레포지토리 단위(### 프로젝트:)로 청킹(Chunking)
    repo_blocks = github_context.split("\n---\n\n")
    chunks = []
    
    current_chunk = ""
    for block in repo_blocks:
        if len(current_chunk) + len(block) > 3000:  # 대략 3000자 단위로 쪼개기
            chunks.append(current_chunk)
            current_chunk = block
        else:
            current_chunk += ("\n---\n\n" + block if current_chunk else block)
    if current_chunk:
        chunks.append(current_chunk)
        
    print(f"\n▶️ 3. LLM 분석 파이프라인 가동 (Github {len(chunks)}개 청크 + Velog 1개) 병렬 처리...")
    llm_start = time.time()
    
    # 청크별로 프롬프트 만들기 (비동기 병렬 실행을 위해 asyncio.to_thread 사용)
    tasks = []
    
    # 3-1. Github 청크들 분석 태스크
    for i, chunk in enumerate(chunks):
        md = f"# [파트 1] Github 커밋 내역 (부분 {i+1})\n{chunk}"
        tasks.append(asyncio.to_thread(analyze_all_activities, md))
        
    # 3-2. Velog 분석 태스크
    md = f"# [파트 2] Velog 기술 블로그 포스팅 내역\n{velog_context}"
    tasks.append(asyncio.to_thread(analyze_all_activities, md))
    
    # 3-3. 1분 걸릴 작업을 15초만에! 싹 다 비동기 병렬 실행 ⚡
    print(f"   => 총 {len(tasks)}개의 LLM 분석 스레드가 동시 출발합니다!")
    chunk_results = await asyncio.gather(*tasks)
    
    # 결과 합치기
    all_activities = []
    for res_list in chunk_results:
        all_activities.extend(res_list)
        
    llm_end = time.time()
    print(f"✅ 개별 활동 분석 완료! (분석 소요 시간: {llm_end - llm_start:.2f}초 / 총 {len(all_activities)}개 항목 획득)")
    
    # 4. 아까 말씀하신 '최종 1페이지 요약' 진행
    print("\n▶️ 4. 확보된 데이터를 바탕으로 최종 프로필 요약을 시작합니다...")
    final_start = time.time()
    
    final_profile = await asyncio.to_thread(generate_final_profile, all_activities)
    
    final_end = time.time()
    
    print(f"✅ 최종 프로필 생성 완료! (소요 시간: {final_end - final_start:.2f}초)")
    print("=" * 60)
    print("🏆 [최종 개발자 프로필]")
    print(json.dumps(final_profile, indent=2, ensure_ascii=False))
    print("=" * 60)
    
    print(f"🚀 [Performance 요약]")
    print(f"- 데이터 수집: {collection_end - collection_start:.2f}초")
    print(f"- 활동 분석(병렬): {llm_end - llm_start:.2f}초")
    print(f"- 최종 요약: {final_end - final_start:.2f}초")
    print(f"- **총 소요 시간**: {final_end - collection_start:.2f}초")
    
    # 5. 생성된 데이터들을 FastAPI 등에서 쓸 수 있도록 Return
    return {
        "profile": final_profile,
        "activities": all_activities
    }
    
    
async def analyze_from_activity_history(activity_records: list[dict | BaseModel]) -> dict:
    """SpringBoot에서 전달받은 activity_history 레코드를 기반으로 FinalProfile을 생성합니다. (기존 온보딩 분기 대체용)"""
    
    # 1. ActivityHistoryItem 포맷을 generate_final_profile이 기대하는 ActivitySummary(JSON 배열) 포맷으로 매핑
    converted_activities = []
    for i, item in enumerate(activity_records):
        act = item.model_dump() if hasattr(item, "model_dump") else item
        
        # 키 호환성 처리 (카멜케이스 or 스네이크케이스)
        act_type = act.get("activityType") or act.get("activity_type") or "추가 활동"
        if act_type == "COMMIT":
            mapped_type = "Github Commit"
        elif act_type == "PR":
            mapped_type = "Github PR"
        elif act_type == "VELOG":
            mapped_type = "Velog Post"
        else:
            mapped_type = "기타 활동"

        converted_activities.append({
            "id": i + 1,
            "type": mapped_type,
            "tech_stacks": act.get("techStacks") or act.get("tech_stacks") or [],
            "summary": act.get("title", ""),
            "category": act.get("category", "개발")
        })

    # 2. 로깅
    print(f"\n▶️ [Activity History 기반 분석] 총 {len(converted_activities)}개 레코드 처리 시작...")
    
    # 3. 기존 FinalProfile 생성 로직 재활용
    final_profile = await asyncio.to_thread(generate_final_profile, converted_activities)
    
    print("✅ Activity History 기반 데이터로 프로필 갱신 완료!")
    
    return {
        "profile": final_profile,
        "activities": converted_activities
    }

if __name__ == "__main__":
    import sys
    if sys.platform == 'win32':
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    
    # 예시 실행용 더미 래퍼
    async def _test():
        # 로컬 테스트 시 여기에 임시값을 넣어서 사용할 수 있습니다.
        # gh_ctx = await start_github_collection("your_token_here", "your_github_username")
        # res = await start_velog_and_analysis("your_velog_username", gh_ctx)
        # print(res)
        pass

    asyncio.run(_test())
