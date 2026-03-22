import pandas as pd
from app.services.qdrant_client import get_qdrant_client
from app.core.settings import QDRANT_COLLECTION_NAME

def check_qdrant_data():
    client = get_qdrant_client()
    
    print(f"[{QDRANT_COLLECTION_NAME}] 컬렉션에서 데이터(Payload)를 가져오는 중...")
    
    records, _ = client.scroll(
        collection_name=QDRANT_COLLECTION_NAME,
        with_payload=True,
        with_vectors=False,  # 벡터는 무거우니 제외
        limit=10000          # 최대 1만 개까지 스크롤
    )
    
    if not records:
        print("데이터가 없습니다.")
        return
        
    # Payload만 추출해서 리스트 딕셔너리로 만듦
    payloads = [r.payload for r in records if r.payload]
    
    # Pandas DataFrame으로 변환!
    df = pd.DataFrame(payloads)
    
    print(f"\n✅ 총 {len(df)}개의 청크 데이터를 DataFrame으로 로드했습니다.\n")
    print("=" * 50)
    
    # 1. title 필드가 없거나 빈 결측치(빈 문자열) 개수
    empty_titles = df['title'].eq('').sum() + df['title'].isna().sum()
    print(f"📌 [Title 검증] 'title' 값이 비어있는 청크 수: {empty_titles} / {len(df)}")
    if empty_titles > 0:
        print("   -> (아래는 제목이 비어있는 원본 URL 목록입니다)")
        print(df[df['title'] == '']['source_url'].unique())
        
    print("\n" + "=" * 50)
    
    # 2. skill 데이터 확인 (리스트 형태이므로 풀어줌)
    # 리스트로 묶인 기술 스택들을 풀어서 단일 값으로 만든 뒤 unique 계산
    all_skills = df['skill'].explode().dropna().unique()
    print(f"📌 [Skill 검증] 등록된 고유 기술 스택 (총 {len(all_skills)}개):")
    for s in sorted(all_skills):
        print(f"   - {s}")
        
    print("=" * 50)
    print("\nDataFrame 상위 3개 미리보기:")
    # skill은 보여주기 편하도록 쉼표로 합침
    df_preview = df.copy()
    df_preview['skill'] = df_preview['skill'].apply(lambda x: ', '.join(x) if isinstance(x, list) else x)
    print(df_preview[['doc_id', 'title', 'skill', 'updated_at']].head(3))


if __name__ == "__main__":
    check_qdrant_data()
