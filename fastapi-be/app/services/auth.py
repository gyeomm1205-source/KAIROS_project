import os
from dataclasses import dataclass
from functools import lru_cache
from typing import Any, Optional

import jwt
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import InvalidTokenError, PyJWKClient
from starlette import status

from app.services.errors import AppError

bearer_scheme = HTTPBearer(auto_error=False)


def _env_bool(name: str, default: bool = False) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class CognitoSettings:
    enabled: bool
    region: str
    user_pool_id: str
    client_id: str
    issuer: str
    token_use: str
    username_claim: str

    @property
    def jwks_url(self) -> Optional[str]:
        if not self.issuer:
            return None
        return f"{self.issuer}/.well-known/jwks.json"

    @property
    def is_configured(self) -> bool:
        return self.enabled and bool(self.issuer and self.client_id and self.token_use)


@dataclass(frozen=True)
class CognitoUser:
    subject: str
    username: Optional[str]
    email: Optional[str]
    groups: list[str]
    token_use: str
    client_id: Optional[str]
    claims: dict[str, Any]

    @classmethod
    def from_claims(cls, claims: dict[str, Any], username_claim: str) -> "CognitoUser":
        username = claims.get(username_claim) or claims.get("cognito:username") or claims.get("email") or claims.get("sub")
        groups_claim = claims.get("cognito:groups")
        groups = [str(value) for value in groups_claim] if isinstance(groups_claim, list) else []
        client_id = claims.get("client_id")

        aud_claim = claims.get("aud")
        if client_id is None and isinstance(aud_claim, str):
            client_id = aud_claim

        return cls(
            subject=str(claims["sub"]),
            username=str(username) if username is not None else None,
            email=str(claims["email"]) if "email" in claims and claims["email"] is not None else None,
            groups=groups,
            token_use=str(claims["token_use"]),
            client_id=str(client_id) if client_id is not None else None,
            claims=claims,
        )


def _resolve_issuer(region: str, user_pool_id: str, configured_issuer: str) -> str:
    if configured_issuer:
        return configured_issuer
    if region and user_pool_id:
        return f"https://cognito-idp.{region}.amazonaws.com/{user_pool_id}"
    return ""


@lru_cache
def get_cognito_settings() -> CognitoSettings:
    region = os.getenv("COGNITO_AWS_REGION", "").strip()
    user_pool_id = os.getenv("COGNITO_USER_POOL_ID", "").strip()
    issuer = _resolve_issuer(region, user_pool_id, os.getenv("COGNITO_ISSUER_URI", "").strip())

    return CognitoSettings(
        enabled=_env_bool("COGNITO_ENABLED", False),
        region=region,
        user_pool_id=user_pool_id,
        client_id=os.getenv("COGNITO_CLIENT_ID", "").strip(),
        issuer=issuer,
        token_use=os.getenv("COGNITO_TOKEN_USE", "access").strip() or "access",
        username_claim=os.getenv("COGNITO_USERNAME_CLAIM", "cognito:username").strip() or "cognito:username",
    )


@lru_cache
def get_cognito_verifier() -> "CognitoTokenVerifier":
    return CognitoTokenVerifier(get_cognito_settings())


class CognitoTokenVerifier:
    def __init__(self, settings: CognitoSettings) -> None:
        self.settings = settings
        self.jwks_client = PyJWKClient(settings.jwks_url) if settings.is_configured and settings.jwks_url else None

    def verify(self, token: str) -> CognitoUser:
        if not self.settings.is_configured or self.jwks_client is None:
            raise AppError(
                status_code=status.HTTP_401_UNAUTHORIZED,
                code="AUTH-001",
                message="Cognito authentication is not configured.",
            )

        try:
            signing_key = self.jwks_client.get_signing_key_from_jwt(token)
            claims = jwt.decode(
                token,
                signing_key.key,
                algorithms=["RS256"],
                issuer=self.settings.issuer,
                options={"verify_aud": False},
            )
        except InvalidTokenError as exc:
            raise AppError(
                status_code=status.HTTP_401_UNAUTHORIZED,
                code="AUTH-001",
                message="Invalid Cognito token.",
            ) from exc
        except Exception as exc:
            raise AppError(
                status_code=status.HTTP_401_UNAUTHORIZED,
                code="AUTH-001",
                message="Unable to validate Cognito token.",
            ) from exc

        self._validate_token_use(claims)
        self._validate_client_id(claims)
        return CognitoUser.from_claims(claims, self.settings.username_claim)

    def _validate_token_use(self, claims: dict[str, Any]) -> None:
        actual = str(claims.get("token_use", ""))
        if actual != self.settings.token_use:
            raise AppError(
                status_code=status.HTTP_401_UNAUTHORIZED,
                code="AUTH-001",
                message=f"Invalid Cognito token_use. Expected {self.settings.token_use}.",
            )

    def _validate_client_id(self, claims: dict[str, Any]) -> None:
        if self.settings.token_use == "id":
            audience = claims.get("aud")
            if audience == self.settings.client_id:
                return
        elif claims.get("client_id") == self.settings.client_id:
            return

        raise AppError(
            status_code=status.HTTP_401_UNAUTHORIZED,
            code="AUTH-001",
            message="Invalid Cognito client identifier.",
        )


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    verifier: CognitoTokenVerifier = Depends(get_cognito_verifier),
) -> CognitoUser:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise AppError(
            status_code=status.HTTP_401_UNAUTHORIZED,
            code="AUTH-001",
            message="Authentication is required.",
        )

    return verifier.verify(credentials.credentials)
