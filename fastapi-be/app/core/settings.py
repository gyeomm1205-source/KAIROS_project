import os
from dotenv import load_dotenv

load_dotenv()

# Qdrant 설정 (EC2 Docker Qdrant 사용)
QDRANT_HOST = os.getenv("QDRANT_HOST", "localhost")
QDRANT_PORT = int(os.getenv("QDRANT_PORT", "6333"))
QDRANT_COLLECTION_NAME = os.getenv("QDRANT_COLLECTION_NAME", "reference_chunks")
