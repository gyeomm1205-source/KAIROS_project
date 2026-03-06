from datetime import datetime

from pydantic import BaseModel


class ErrorResponse(BaseModel):
    code: str
    message: str
    timestamp: datetime
    path: str
