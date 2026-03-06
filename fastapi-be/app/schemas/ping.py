from datetime import datetime
from typing import Any, Optional

from pydantic import BaseModel


class PingResponse(BaseModel):
    source: str
    message: str
    timestamp: datetime
    downstream: Optional[Any] = None
