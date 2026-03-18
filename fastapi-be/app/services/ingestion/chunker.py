"""
텍스트를 Qdrant에 업로드하기 적합한 크기로 분할하는 청커.
"""

CHUNK_SIZE = 500      # 한 청크의 최대 글자 수
CHUNK_OVERLAP = 100   # 인접 청크 간 오버랩 글자 수
MIN_CHUNK_SIZE = 80   # 이보다 짧은 청크는 버림


def split_into_chunks(text: str) -> list[str]:
    """
    텍스트를 CHUNK_SIZE 단위로 분할합니다.
    문장 경계(마침표/줄바꿈)를 최대한 존중합니다.
    """
    if not text or len(text) < MIN_CHUNK_SIZE:
        return []

    chunks: list[str] = []
    start = 0
    length = len(text)

    while start < length:
        end = min(start + CHUNK_SIZE, length)

        # 청크 끝이 텍스트 중간이면 자연스러운 경계 탐색
        if end < length:
            # 줄바꿈 또는 마침표 기준으로 뒤로 탐색
            boundary = max(
                text.rfind("\n", start, end),
                text.rfind(". ", start, end),
                text.rfind("。", start, end),
            )
            if boundary > start + MIN_CHUNK_SIZE:
                end = boundary + 1

        chunk = text[start:end].strip()
        if len(chunk) >= MIN_CHUNK_SIZE:
            chunks.append(chunk)

        next_start = end - CHUNK_OVERLAP
        # OOM 방지: start가 무조건 전진하도록 보장 (중복 루프 방지)
        start = max(next_start, start + 1)

    return chunks
