"""
pytest conftest — env var setup before any service imports.

profile_analyzer.py hardcodes the GMS credentials, but quiz_svc / recommendation_svc
use model_router.py which reads OPENAI_API_KEY from os.environ at import time.
Setting them here guarantees they are available before any module-level reads.
"""

import os

os.environ.setdefault("OPENAI_API_KEY", "S14P22A506-20649484-f08a-4522-a9fe-a26f5b4a9246")
os.environ.setdefault("OPENAI_API_BASE", "https://gms.ssafy.io/gmsapi/api.openai.com/v1")
