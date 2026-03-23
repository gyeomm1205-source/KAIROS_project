import os
import asyncio
import sys
from dotenv import load_dotenv

# 현재 스크립트의 경로를 sys.path에 추가 (app 모듈을 찾을 수 있도록 함)
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# .env 파일 로드
load_dotenv()

from app.services.profile_analyzer import start_github_collection, start_velog_and_analysis

async def test_real_profile_analyzer():
    github_token = os.getenv("GITHUB_TEST_TOKEN")
    github_username = os.getenv("GITHUB_TEST_USERNAME")
    velog_username = github_username # 임시로 동일하게 설정
    
    if not github_token or not github_username:
        print("❌ .env 파일에 GITHUB_TEST_TOKEN 또는 GITHUB_TEST_USERNAME이 없습니다.")
        return
        
    print("\n" + "="*60)
    print(f"🚀 실제 GitHub/Velog 계정을 이용한 프로필 분석 테스트 시작")
    print(f"대상 계정: GitHub({github_username}), Velog({velog_username})")
    print("="*60 + "\n")
    
    try:
        # 1. 깃허브 데이터 수집
        github_context = await start_github_collection(github_token, github_username)
        
        # 2. 벨로그 수집 및 LLM 분석
        # (안에 출력문이 내장되어 있으므로 터미널에서 진행상황 확인 가능)
        results = await start_velog_and_analysis(velog_username, github_context)
        
        print("\n🎉 모든 분석이 성공적으로 실행되었습니다.")
    except Exception as e:
        print(f"\n❌ 실행 중 에러 발생: {e}")

if __name__ == "__main__":
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    asyncio.run(test_real_profile_analyzer())
