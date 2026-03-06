from typing import Optional

from pydantic import BaseModel


class CurrentUserResponse(BaseModel):
    source: str
    subject: str
    username: Optional[str] = None
    email: Optional[str] = None
    groups: list[str] = []
    token_use: str
    client_id: Optional[str] = None
