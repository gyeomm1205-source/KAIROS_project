import aiohttp
import asyncio
import json
import csv
import io
import time
import re

# ==========================================
# [설정] 본인의 Velog 닉네임 입력 (예: zhy2on)
# velog.io/@username 에서 username 부분
# 테스트 시 아래 주석을 풀고 넘겨받은 파라미터 대신 사용할 수 있습니다.
# ==========================================
# VELOG_USERNAME = "zhy2on"

# Velog는 GraphQL API를 사용합니다.
GRAPHQL_URL = "https://v2.velog.io/graphql"

async def fetch_velog_posts(session, username):
    """Velog GraphQL API를 통해 유저의 게시글 목록을 가져옵니다."""
    print(f"🔍 [{username}] 님의 Velog 게시글 목록을 가져옵니다...")
    
    # GraphQL 쿼리: 유저의 게시물 목록 (최신순)
    query = """
    query Posts($username: String!) {
      posts(username: $username) {
        id
        title
        short_description
        body
        tags
        released_at
      }
    }
    """
    
    variables = {
        "username": username
    }
    
    async with session.post(GRAPHQL_URL, json={"query": query, "variables": variables}) as response:
        if response.status == 200:
            data = await response.json()
            posts = data.get("data", {}).get("posts", [])
            print(f"✅ 총 {len(posts)}개의 Velog 게시글을 찾았습니다.")
            return posts
        else:
            print(f"❌ Velog API 호출 실패: 상태 코드 {response.status}")
            return []

def clean_text(text):
    """마크다운/HTML 태그를 제거하고 읽기 쉬운 텍스트로 만듭니다."""
    if not text:
        return ""
    # 정규식으로 한글, 영문, 숫자, 공백만 남기기
    cleaned = re.sub(r'[^가-힣a-zA-Z0-9\s]', ' ', text)
    # 연속된 공백 압축
    return ' '.join(cleaned.split())

def convert_velog_to_markdown(posts, velog_username: str):
    """수집된 Velog 데이터를 LLM이 좋아하는 마크다운+CSV 형태로 변환합니다."""
    
    md = f"### 블로그: Velog (@{velog_username})\n"
    md += f"* **블로그 특징**: 개발자 기술 블로그 (마크다운 기반 포스팅)\n"
    md += f"* **작업 데이터 (CSV 형식)**:\n"
    
    csv_io = io.StringIO()
    writer = csv.writer(csv_io)
    writer.writerow(['id', 'date', 'activity_type', 'title', 'tags', 'content_snippet'])
    
    for idx, post in enumerate(posts):
        date = post.get('released_at', '')[:10]  # YYYY-MM-DD
        title = post.get('title', '')
        tags = ", ".join(post.get('tags', []))
        
        # short_description을 쓰거나 본문(body)을 잘라서 요약으로 사용
        snippet = post.get('short_description', '')
        if not snippet and post.get('body'):
            snippet = clean_text(post.get('body'))[:150] + "..."
            
        writer.writerow([
            idx + 1,
            date,
            "Blog Post",
            title,
            tags,
            clean_text(snippet)
        ])
        
    md += csv_io.getvalue()
    return md

async def main(velog_username: str):
    start_time = time.time()
    
    # @zhy2on 처럼 @ 기호 포함해서 입력해도 자동으로 처리
    velog_username = velog_username.lstrip('@').strip()
    
    async with aiohttp.ClientSession() as session:
        posts = await fetch_velog_posts(session, velog_username)
        
    collection_time = time.time()
    print(f"\n⏱️ Velog 데이터 수집 소요 시간: {collection_time - start_time:.2f}초\n")
    
    if not posts:
        return None
        
    # 메모리에 올릴 문자열 포맷팅
    formatted_text = convert_velog_to_markdown(posts, velog_username)
    return formatted_text

if __name__ == "__main__":
    import sys
    if sys.platform == 'win32':
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
        
    # 테스트 실행 시 프롬프트 출력
    # 테스트 시에는 실제 유저네임을 넣어주세요. (혹은 주석 처리된 변수를 사용)
    # VELOG_USERNAME = "zhy2on"
    # formatted_text = asyncio.run(main(VELOG_USERNAME))
    formatted_text = None
    
    if formatted_text:
        prompt = f"""너는 시니어 개발자 프로필 분석 AI '카이로스'야.
아래의 [활동 내역]은 유저가 작성한 기술 블로그(Velog) 포스팅 정보야.
각 포스팅의 제목, 태그, 본문 요약을 바탕으로 아래 규칙을 무조건 지켜서 JSON을 생성해.

<규칙>
1. tech_stacks: 태그(tags)와 본문 요약에서 추출한 핵심 기술 스택. 'velog', '개발' 같은 추상적인 단어 제외.
2. summary: 포스팅 내용을 바탕으로 '무엇을 학습했는지/어떤 트러블슈팅을 했는지' 시니어 관점으로 2줄 요약할 것.
3. category: 포스팅은 '학습', '개발','취준', '기타' 중 하나로 분류할 것.

[활동 내역(컨텍스트)]
{formatted_text}

[Output JSON 형식]
[
  {{
    "id": 1,
    "tech_stacks": ["NestJS", "Redis"],
    "summary": "NestJS 환경에서 Redis를 활용한 세션 관리 전략 학습",
    "category": "학습"
  }}
]"""

        print("="*65)
        print("✨ 완성된 Velog Batch 프롬프트 (메모리상 로드 직전)")
        print("="*65 + "\n")
        print(prompt)
