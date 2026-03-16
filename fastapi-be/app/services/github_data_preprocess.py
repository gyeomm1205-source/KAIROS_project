import aiohttp
import asyncio
import json
import csv
import io
import os
import time
import re
from dotenv import load_dotenv

load_dotenv()

# ==========================================
# [Spring Boot가 FastAPI로 넘겨주었다고 가정하는 데이터]
# GitHub OAuth 구현 전 임시 테스트용 — .env 파일에서 로드
# ==========================================
GITHUB_TOKEN = os.getenv("GITHUB_TEST_TOKEN", "")
USERNAME = os.getenv("GITHUB_TEST_USERNAME", "")

HEADERS = {
    "Authorization": f"token {GITHUB_TOKEN}",
    "Accept": "application/vnd.github.v3+json"
}

async def get_user_repositories(session):
    """1. 유저가 소유하거나 참여 중인(팀/조직 포함) 모든 레포지토리를 최근 업데이트 순으로 가져옵니다."""
    print("🔍 유저가 참여 중인 레포지토리 목록을 가져옵니다...")
    repos = []
    page = 1
    while True:
        url = f"https://api.github.com/user/repos?sort=updated&per_page=100&page={page}"
        async with session.get(url, headers=HEADERS) as response:
            if response.status == 200:
                data = await response.json()
                if not data:
                    break
                repos.extend(data)
                page += 1
            else:
                break
    print(f"✅ 총 {len(repos)}개의 레포지토리를 찾았습니다.")
    return repos

async def fetch_changed_files(session, files_url):
    """수정된 파일명 목록을 가져옵니다."""
    async with session.get(files_url, headers=HEADERS) as res:
        if res.status == 200:
            data = await res.json()
            return [f["filename"] for f in data if "filename" in f][:5]
    return []

async def fetch_readme(session, repo_owner, repo_name):
    """README 파일을 가져와서 정규식으로 정제한 후 첫 150자만 추출합니다."""
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/readme"
    headers = HEADERS.copy()
    headers["Accept"] = "application/vnd.github.v3.raw"
    async with session.get(url, headers=headers) as res:
        if res.status == 200:
            text = await res.text()
            # 정규식으로 한글, 영문, 숫자, 공백만 남기고 지우기 (마크다운 기호 등 토큰 낭비 제거)
            clean_text = re.sub(r'[^가-힣a-zA-Z0-9\s]', ' ', text)
            # 연속된 공백을 하나로 압축하고 150자로 자르기
            summary = ' '.join(clean_text.split())[:150]
            return summary + ("..." if len(summary) >= 150 else "")
    return "README 정보 없음"

async def fetch_dependencies(session, repo_owner, repo_name):
    """설정 파일들을 분석해 기술 스택을 가져옵니다 (최대 15개)."""
    headers = HEADERS.copy()
    headers["Accept"] = "application/vnd.github.v3.raw"
    
    # 1. package.json (Node.js/React/Vue 계열)
    url_pkg = f"https://api.github.com/repos/{repo_owner}/{repo_name}/contents/package.json"
    async with session.get(url_pkg, headers=headers) as res:
        if res.status == 200:
            try:
                text = await res.text()
                pkg = json.loads(text)
                deps = list(pkg.get("dependencies", {}).keys())
                dev_deps = list(pkg.get("devDependencies", {}).keys())
                all_deps = deps + dev_deps
                if all_deps:
                    return f"NPM 패키지 ({', '.join(all_deps[:15])})"
            except:
                pass

    # 2. requirements.txt (Python 계열)
    url_req = f"https://api.github.com/repos/{repo_owner}/{repo_name}/contents/requirements.txt"
    async with session.get(url_req, headers=headers) as res:
        if res.status == 200:
            text = await res.text()
            lines = [line.split("==")[0].strip() for line in text.split("\n") if line and not line.startswith("#")]
            if lines:
                return f"Python 패키지 ({', '.join(lines[:15])})"

    # 3. build.gradle (Java/Kotlin, Spring Boot 계열)
    url_gradle = f"https://api.github.com/repos/{repo_owner}/{repo_name}/contents/build.gradle"
    async with session.get(url_gradle, headers=headers) as res:
        if res.status == 200:
            text = await res.text()
            # implementation 'org.springframework.boot:spring-boot-starter-web' 패턴 등
            deps = re.findall(r"['\"]([^'\"]+:[^'\"]+)['\"]", text)
            if deps:
                # 'org.springframework.boot:spring-boot-starter-web' 에서 뒤에 아티팩트명만 추출
                clean_deps = [d.split(':')[-1] for d in deps if ':' in d]
                if clean_deps:
                    return f"Gradle 의존성 ({', '.join(clean_deps[:15])})"

    # 4. pom.xml (Java, Maven 계열)
    url_pom = f"https://api.github.com/repos/{repo_owner}/{repo_name}/contents/pom.xml"
    async with session.get(url_pom, headers=headers) as res:
        if res.status == 200:
            text = await res.text()
            deps = re.findall(r"<artifactId>([^<]+)</artifactId>", text)
            if deps:
                # 기본적인 속성들 필터링 후 최대 15개
                clean_deps = [d for d in deps if not d.endswith('.version') and d not in ('project', 'modelVersion')]
                if clean_deps:
                    return f"Maven 의존성 ({', '.join(clean_deps[:15])})"
    
    # 5. go.mod (Go 계열)
    url_go = f"https://api.github.com/repos/{repo_owner}/{repo_name}/contents/go.mod"
    async with session.get(url_go, headers=headers) as res:
        if res.status == 200:
            text = await res.text()
            deps = []
            in_require = False
            for line in text.split('\n'):
                line = line.strip()
                if line.startswith('require ('):
                    in_require = True
                    continue
                if in_require and line == ')':
                    break
                if in_require and line:
                    deps.append(line.split()[0].split('/')[-1])
                elif line.startswith('require '):
                    deps.append(line.split()[1].split('/')[-1])
            if deps:
                return f"Go 모듈 ({', '.join(deps[:15])})"

    # 6. 둘 다 없으면 Github 언어 통계로 대체
    url_lang = f"https://api.github.com/repos/{repo_owner}/{repo_name}/languages"
    async with session.get(url_lang, headers=HEADERS) as res:
         if res.status == 200:
             data = await res.json()
             if data:
                 return f"주요 언어: {', '.join(list(data.keys())[:5])}"
    
    return "의존성 정보 없음"

async def process_repository(session, repo):
    """2. 레포지토리 1개를 분석하여 PR 또는 Commit 단위의 활동(Activity)을 추출하고 메타데이터를 덧붙입니다."""
    repo_owner = repo['owner']['login']
    repo_name = repo['name']
    repo_full_name = repo['full_name']
    
    print(f"📂 분석 중: [{repo_full_name}]")
    
    activities = []

    # [로직 A] PR 가져오기
    pr_search_url = f"https://api.github.com/search/issues?q=repo:{repo_full_name}+is:pr+is:merged+author:{USERNAME}"
    async with session.get(pr_search_url, headers=HEADERS) as pr_res:
        if pr_res.status == 200:
            data = await pr_res.json()
            prs = data.get("items", [])
        else:
            prs = []
    
    if len(prs) > 0:
        print(f"   [{repo_name}] => 👥 PR {len(prs)}개 발견! (수집 시작)")
        
        async def fetch_pr_files(pr):
            pr_num = pr["number"]
            files_url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/pulls/{pr_num}/files"
            files = await fetch_changed_files(session, files_url)
            return {
                "date": pr["closed_at"][:10],
                "type": "PR",
                "title": pr["title"],
                "files": files
            }
        
        pr_tasks = [fetch_pr_files(pr) for pr in prs[:3]]
        activities = await asyncio.gather(*pr_tasks)
    
    # [로직 B] PR이 없으면 Commit 가져오기
    else:
        print(f"   [{repo_name}] => ⚠️ PR 없음! 직접 푸시한 Commit 내역을 수집합니다.")
        commits_url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/commits?author={USERNAME}&per_page=3"
        async with session.get(commits_url, headers=HEADERS) as commits_res:
            if commits_res.status == 200:
                commits = await commits_res.json()
                
                async def fetch_commit_detail(c):
                    sha = c["sha"]
                    msg = c["commit"]["message"].split('\n')[0]
                    date = c["commit"]["author"]["date"][:10]
                    
                    detail_url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/commits/{sha}"
                    async with session.get(detail_url, headers=HEADERS) as detail_res:
                        files = []
                        if detail_res.status == 200:
                            data = await detail_res.json()
                            files = [f["filename"] for f in data.get("files", [])][:5]
                    
                    return {
                        "date": date,
                        "type": "Commit",
                        "title": msg,
                        "files": files
                    }
                
                commit_tasks = [fetch_commit_detail(c) for c in commits]
                activities = await asyncio.gather(*commit_tasks)

    # 활동 내역이 없으면 통과
    if not activities:
        return None
        
    # 레포지토리 메타데이터 (README, 의존성) 수집 - 여기서 추가됩니다!
    readme = await fetch_readme(session, repo_owner, repo_name)
    dependencies = await fetch_dependencies(session, repo_owner, repo_name)
    
    return {
        "repo_name": repo_name,
        "readme": readme,
        "dependencies": dependencies,
        "activities": activities
    }

def convert_to_markdown_format(repo_data_list):
    """3. 수집된 정보를 LLM 프롬프트용 마크다운 형태로 변환합니다."""
    output = []
    global_id = 1
    
    for data in repo_data_list:
        md = f"### 프로젝트: {data['repo_name']}\n"
        md += f"* **README 요약**: {data['readme']}\n"
        md += f"* **사용 기술(의존성)**: {data['dependencies']}\n"
        md += f"* **작업 데이터 (CSV 형식)**:\n"
        
        csv_io = io.StringIO()
        writer = csv.writer(csv_io)
        writer.writerow(['id', 'date', 'activity_type', 'title_or_message', 'changed_files'])
        
        for act in data['activities']:
            writer.writerow([
                global_id,
                act['date'],
                act['type'],
                act['title'],
                ", ".join(act['files'])
            ])
            global_id += 1
            
        md += csv_io.getvalue()
        output.append(md)
        
    return "\n---\n\n".join(output)

async def main():
    start_time = time.time()
    
    async with aiohttp.ClientSession() as session:
        repos = await get_user_repositories(session)
        if not repos:
            print("❌ 접근 가능한 레포지토리가 없거나 토큰이 유효하지 않습니다.")
            return None
            
        tasks = [process_repository(session, repo) for repo in repos]
        results = await asyncio.gather(*tasks)
        
        # 데이터가 있는 레포지토리만 리스트에 담기
        repo_data_list = [res for res in results if res]

    collection_time = time.time()
    print(f"\n⏱️ github 및 메타데이터 수집 소요 시간: {collection_time - start_time:.2f}초")
    
    # 마크다운 텍스트 생성 후 리턴 (메모리 로드용)
    formatted_text = convert_to_markdown_format(repo_data_list)
    return formatted_text

if __name__ == "__main__":
    import sys
    if sys.platform == 'win32':
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    
    # 테스트 실행 시 프롬프트 출력
    formatted_text = asyncio.run(main())
    
    if formatted_text:
        prompt = f"""너는 개발자 활동 분석 AI '카이로스'야.
아래는 유저가 최근 여러 레포지토리에서 작업한 활동 내역이야.
각 레포지토리의 README와 의존성 정보를 바탕으로 전체 컨텍스트를 파악한 뒤, 
아래 CSV에 나열된 커밋/PR(id별)로 어떤 기술을 사용해서 무슨 작업을 했는지 분석하고, 개발/학습/취준/기타로 분류해서 JSON 배열로만 반환해줘.

{formatted_text}

[Output JSON 형식]
[
  {{
    "id": 1,
    "tech_stacks": ["React", "TypeScript", "Socket.io"],
    "summary": "방 관련 이벤트 처리를 위해 소켓 추가 로직 구현",
    "category": "개발"
  }}
]"""

        print("\n" + "="*65)
        print("✨ 완성된 LLM Batch 프롬프트 (레포 단위 컨텍스트 추가)")
        print("="*65 + "\n")
        print(prompt)
