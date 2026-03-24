import os

import httpx
from starlette import status

from app.services.errors import AppError


class SpringClient:
    def __init__(self) -> None:
        self.base_url = os.getenv("SPRING_BASE_URL", "http://localhost:8080")

    async def ping(self) -> dict:
        endpoint = f"{self.base_url}/api/test/ping"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(endpoint)
                response.raise_for_status()
                return response.json()
        except Exception as exc:
            raise AppError(
                status_code=status.HTTP_502_BAD_GATEWAY,
                code="TEST-PING-002",
                message="Spring Boot is unavailable.",
            ) from exc
