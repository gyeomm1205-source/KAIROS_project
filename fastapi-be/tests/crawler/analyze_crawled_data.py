import asyncio
import pandas as pd
import numpy as np

from app.services.ingestion.discovery import discover_articles
from app.services.ingestion.crawler import crawl_all
from app.services.ingestion.seeds import ALL_SEEDS

async def main():
    print("=== 1단계: 전체 블로그 디스커버리 (URL 수집) ===")
    articles = await discover_articles(ALL_SEEDS)
    
    print(f"\n=== 2단계: 크롤러 (총 {len(articles)}개 본문 수집) ===")
    print("크롤링이 진행 중입니다. 시간이 다소 소요될 수 있습니다...")
    crawled = await crawl_all(articles)
    
    # DataFrame으로 변환
    df = pd.DataFrame(crawled)
    
    print("\n================ [결과 저장] ================")
    csv_path = "crawled_summary.csv"
    
    # 스킬 리스트를 문자열로 변환해 Excel에서 보기 편하게 만듦
    if 'skill' in df.columns:
        df['skill_str'] = df['skill'].apply(lambda x: ", ".join(x) if isinstance(x, list) else str(x))
    
    # CSV 저장 (한글 깨짐 방지를 위해 utf-8-sig 사용)
    df.to_csv(csv_path, index=False, encoding='utf-8-sig')
    print(f"'{csv_path}' 파일로 전체 데이터가 저장되었습니다! 엑셀에서 확인해 보세요.")

    print("\n================ [데이터 통계 요약] ================")
    # 빈 문자열("")이나 공백만 있는 경우를 실제 결측치(NaN)로 변환
    df.replace(r'^\s*$', np.nan, regex=True, inplace=True)
    
    print("\n[전체 데이터프레임 결측값(빈 값) 개수 확인]")
    print(df[['source_type', 'title', 'url', 'raw_text', 'published_at']].isna().sum())
    
    if 'source_type' in df.columns:
        print("\n================ [소스(블로그)별 결측치 뜯어보기] ================")
        for source in df['source_type'].dropna().unique():
            sub_df = df[df['source_type'] == source]
            print(f"\n▶ {source} (수집된 글: {len(sub_df)}개)")
            print(sub_df[['title', 'raw_text', 'url', 'published_at']].isna().sum())
            
            # 만약 우아한형제들(woowa)에서 제목이 비어있다면, 제목이 비어있는 URL들의 목록을 살짝 출력
            if source == "tech_blog_woowa":
                missing_titles = sub_df[sub_df['title'].isna()]
                if not missing_titles.empty:
                    print(f"  🚨 우아한형제들 제목 누락 URL 샘플 ({len(missing_titles)}개):")
                    for _, row in missing_titles.head(3).iterrows():
                        print(f"    - {row['url']}")

if __name__ == "__main__":
    asyncio.run(main())
