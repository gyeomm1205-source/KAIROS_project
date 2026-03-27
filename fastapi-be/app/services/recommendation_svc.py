"""
추천 서비스

[기존] POST /api/v1/ai/recommendations/generate     — 하위 호환 유지
[신규] POST /api/v1/ai/recommendations/daily-generate — 자정 배치 / 온보딩 직후 통합 payload 생성

신규 흐름 (run_daily_recommendation):
    1. _build_daily_query()            ← skill_stats + recent_activities 기반 Qdrant 쿼리 생성
    2. _retrieve_from_qdrant_raw()     ← Qdrant 후보 검색
    3. _generate_daily_with_llm()      ← 단일 LLM 호출: 추천이유 + 현상태 + 다음노드 동시 생성
    4. _assemble_daily_response()      ← DailyRecommendationPayload 조립

소유 규칙:
  - 이 서비스는 MySQL에 직접 쿼리하지 않는다.
  - 모든 도메인 컨텍스트는 Request를 통해 전달된다 (Spring Boot 조립).
"""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Any
import json

from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field

from app.core.model_router import TaskType, get_model, get_model_name, get_prompt_version
from app.core.settings import QDRANT_COLLECTION_NAME
from app.models.schemas import (
    ActivityHistoryItem,
    CurrentStatus,
    DailyRecommendationPayload,
    DailyRecommendationRequest,
    RecommendationNextNode,
    RecommendationReason,
    RecommendationReference,
    RecommendationRequest,
    RecommendationResponse,
    SkillStat,
    UserLevel,
)
from app.services.qdrant_client import get_qdrant_client

logger = logging.getLogger(__name__)

# Qdrant 검색 최대 청크 수
_MAX_CANDIDATES = 10

MOCK_DATA_PATH = Path(__file__).parent.parent.parent / "tests" / "mock_recommendation_data.json"

# UserLevel → 난이도 레이블
_LEVEL_LABEL: dict[UserLevel, str] = {
    UserLevel.junior: "입문~초급",
    UserLevel.mid:    "중급",
    UserLevel.senior: "고급~심화",
}


# ---------------------------------------------------------------------------
# LLM 구조화 출력 스키마
# ---------------------------------------------------------------------------

class _NextNodeItem(BaseModel):
    title: str = Field(description="다음 학습 단계 제목. 구체적인 기술 주제로 작성.")


class _RecommendationOutput(BaseModel):
    """LLM 구조화 출력 스키마."""
    summary: str = Field(
        description="추천 이유 요약 한 문장. 사용자 수준과 기술 스택을 언급. 예: '현재 Java 숙련도 기반으로 Spring 심화 자료를 추천합니다.'"
    )
    detail: str = Field(
        description="추천 상세 설명. 분석 결과와 추천 근거를 구체적으로 2~3문장으로 작성."
    )
    next_nodes: list[_NextNodeItem] = Field(
        description="다음 학습 경로 노드 목록. 2~4개. 추천 자료와 연결되는 구체적인 학습 주제."
    )


# ---------------------------------------------------------------------------
# 프롬프트
# ---------------------------------------------------------------------------

_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
개발자의 수준과 관심 기술 스택을 분석해 맞춤형 학습 추천을 제공합니다.

[응답 원칙]
1. summary는 한 문장으로 추천 핵심을 전달하세요.
2. detail은 사용자 수준({user_level})에 맞는 구체적 근거를 포함하세요.
3. next_nodes는 추천 자료와 자연스럽게 이어지는 2~4개 학습 주제를 제시하세요.
4. 반드시 한국어로 작성하세요.\
"""

_USER_TEMPLATE = """\
[사용자 정보]
- 현재 수준: {user_level}
- 관심 기술 스택: {tech_stacks}
- 최근 학습 커리큘럼: {recent_curricula}

[추천 후보 자료]
{reference_list}

위 자료를 바탕으로 추천 이유와 다음 학습 경로를 생성하세요.\
"""


# ---------------------------------------------------------------------------
# 공개 엔트리 포인트
# ---------------------------------------------------------------------------

async def run_recommendation(request: RecommendationRequest) -> RecommendationResponse:
    """POST /api/v1/ai/recommendations/generate 메인 엔트리 포인트."""
    logger.info(
        "[run_recommendation] user_id=%s level=%s stacks=%s",
        request.user_id,
        request.current_level.value,
        request.favorite_tech_stacks,
    )

    # 1. Qdrant에서 후보 자료 검색
    candidates = _retrieve_from_qdrant(request)

    if not candidates:
        logger.warning("[run_recommendation] 후보 없음 — 목 데이터 폴백")
        candidates = _load_mock_candidates()

    # 2. LLM으로 추천 이유 + 다음 학습 경로 생성
    try:
        output = await _generate_reason_with_llm(request, candidates)
    except Exception as exc:
        logger.warning("[run_recommendation] LLM 호출 실패 (%s) — 템플릿 폴백", exc)
        output = _generate_reason_with_template(request, candidates)

    # 3. 응답 조립
    response = _assemble_response(request, candidates, output)

    logger.info("[run_recommendation] 완료 — references=%d next_nodes=%d",
                len(response.references), len(response.next_nodes))
    return response


# ---------------------------------------------------------------------------
# 내부 구현
# ---------------------------------------------------------------------------

def _retrieve_from_qdrant(request: RecommendationRequest) -> list[dict[str, Any]]:
    """favoriteTechStacks 기반으로 Qdrant에서 참고 자료를 검색한다."""
    if not request.favorite_tech_stacks:
        return []

    query_text = " ".join(request.favorite_tech_stacks[:3]) + " 학습 자료"
    level_hint = _LEVEL_LABEL[request.current_level]
    query_text += f" {level_hint}"

    try:
        client = get_qdrant_client()
        results = client.query(
            collection_name=QDRANT_COLLECTION_NAME,
            query_text=query_text,
            limit=_MAX_CANDIDATES,
        )
        candidates = [
            {
                "title":          hit.metadata.get("title", ""),
                "url":            hit.metadata.get("url", "") or hit.metadata.get("source_url", ""),
                "source_type":    hit.metadata.get("source_type", "tech_blog"),
                "published_at":   hit.metadata.get("published_at", None),
                "skill_tags":     hit.metadata.get("skill_tags", []) or hit.metadata.get("skill", []),
                "recommendation_reason": "",  # LLM이 채워줌
                "score":          hit.score,
            }
            for hit in results
            if hit.metadata.get("title")
        ]
        logger.info("[_retrieve_from_qdrant] %d개 후보 검색", len(candidates))
        return candidates

    except Exception as exc:
        logger.warning("[_retrieve_from_qdrant] Qdrant 연결 불가 (%s)", exc)
        return []


async def _generate_reason_with_llm(
    request: RecommendationRequest,
    candidates: list[dict[str, Any]],
) -> _RecommendationOutput:
    """gpt-4o structured output으로 추천 이유와 다음 노드를 생성한다."""
    llm = get_model(TaskType.RECOMMENDATION_REASON, temperature=0.3)
    structured_llm = llm.with_structured_output(_RecommendationOutput)

    prompt = ChatPromptTemplate.from_messages([
        ("system", _SYSTEM_PROMPT),
        ("human",  _USER_TEMPLATE),
    ])
    chain = prompt | structured_llm

    result: _RecommendationOutput = await chain.ainvoke({
        "user_level":       _LEVEL_LABEL[request.current_level],
        "tech_stacks":      ", ".join(request.favorite_tech_stacks) or "미지정",
        "recent_curricula": _fmt_recent_curricula(request.recent_curricula_ids),
        "reference_list":   _fmt_candidates(candidates[:5]),
    })

    logger.debug(
        "[_generate_reason_with_llm] model=%s summary_length=%d next_nodes=%d",
        get_model_name(TaskType.RECOMMENDATION_REASON),
        len(result.summary),
        len(result.next_nodes),
    )
    return result


def _generate_reason_with_template(
    request: RecommendationRequest,
    candidates: list[dict[str, Any]],
) -> _RecommendationOutput:
    """규칙 기반 폴백 — LLM 호출 없음."""
    stacks = ", ".join(request.favorite_tech_stacks[:2]) or "개발"
    level  = _LEVEL_LABEL[request.current_level]

    summary = f"{stacks} {level} 수준에 적합한 자료를 추천합니다."
    detail  = (
        f"현재 {level} 수준에서 {stacks} 역량을 강화할 수 있는 자료를 선별했습니다. "
        "제공된 자료를 순서대로 학습하면 효과적입니다."
    )
    next_nodes = [
        _NextNodeItem(title=f"{s} 심화 학습")
        for s in request.favorite_tech_stacks[:3]
    ] or [_NextNodeItem(title="개발 역량 강화")]

    return _RecommendationOutput(summary=summary, detail=detail, next_nodes=next_nodes)


def _assemble_response(
    request: RecommendationRequest,
    candidates: list[dict[str, Any]],
    output: _RecommendationOutput,
) -> RecommendationResponse:
    """최종 RecommendationResponse를 조립한다."""
    references = [
        RecommendationReference(
            title=c["title"],
            recommendation_reason=_item_reason(c, output.summary),
            reference_type=_map_source_type(c.get("source_type", "")),
            published_at=c.get("published_at"),
            url=c.get("url", ""),
            tech_stacks=c.get("skill_tags", []),
        )
        for c in candidates[:5]
        if c.get("title") and c.get("url")
    ]

    next_nodes = [
        RecommendationNextNode(title=n.title)
        for n in output.next_nodes
    ]

    return RecommendationResponse(
        user_id=request.user_id,
        recommendation_reason=RecommendationReason(
            summary=output.summary,
            detail=output.detail,
        ),
        next_nodes=next_nodes,
        references=references,
    )


# ---------------------------------------------------------------------------
# 헬퍼
# ---------------------------------------------------------------------------

def _load_mock_candidates() -> list[dict[str, Any]]:
    """테스트용 목 데이터 로드. Qdrant 연결 불가 시 사용."""
    try:
        with MOCK_DATA_PATH.open(encoding="utf-8") as f:
            data = json.load(f)
        hits = data.get("mock_qdrant_hits", [])
        return [
            {
                "title":       h.get("title", ""),
                "url":         h.get("url", ""),
                "source_type": h.get("source_type", "tech_blog"),
                "published_at": h.get("published_at", None),
                "skill_tags":  h.get("skill_tags", []),
                "score":       h.get("score", 0.0),
            }
            for h in hits
        ]
    except Exception:
        return []


def _fmt_recent_curricula(ids: list[int]) -> str:
    if not ids:
        return "없음"
    return ", ".join(f"커리큘럼 #{i}" for i in ids)


def _fmt_candidates(candidates: list[dict[str, Any]]) -> str:
    if not candidates:
        return "검색 결과 없음"
    lines = []
    for c in candidates:
        tags = ", ".join(c.get("skill_tags", []))
        lines.append(f"- {c['title']} (태그: {tags})")
    return "\n".join(lines)


def _item_reason(candidate: dict[str, Any], default: str) -> str:
    """개별 레퍼런스 추천 이유를 반환한다."""
    tags = candidate.get("skill_tags", [])
    if tags:
        return f"{', '.join(tags[:2])} 관련 학습에 적합한 자료입니다."
    return default


def _map_source_type(source_type: str) -> str:
    return mapping.get(source_type.lower(), "TECH_BLOG")


# ---------------------------------------------------------------------------
# [신규] 자정 배치 / 온보딩 직후 통합 추천 파이프라인
# ---------------------------------------------------------------------------

class _DailyRecommendationOutput(BaseModel):
    """자정 배치용 통합 LLM 구조화 출력 스키마."""
    reason_summary: str = Field(description="추천 이유 핵심 1문장")
    reason_detail: str = Field(description="추천 구체적 배경 2~3문장")
    status_summary: str = Field(description="현재 학습 상태 핵심 요약 1문장")
    status_detail: str = Field(description="최근 활동과 통계 기반의 현재 상태 상세 분석 2~3문장")
    top_skills: list[str] = Field(description="현재 학습 상태를 나타내는 핵심 기술 키워드 1~3개")
    next_nodes: list[_NextNodeItem] = Field(description="다음 추천 학습 경로 2~4개")


_DAILY_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
사용자의 [최근 활동], [기술 숙련도 통계], [현재 수준]을 종합적으로 분석하여,
1. 사용자의 '현재 학습 상태'를 명확히 진단하고
2. 이를 바탕으로 '맞춤형 추천 이유'와 '다음 학습 경로'를 제안합니다.

[응답 작성 원칙]
- status_summary/detail: 최근 활동의 맥락(어떤 기술을 주로 썼는지)을 포함하여 현재 상태를 진단하세요.
- reason_summary/detail: 진단된 상태를 바탕으로 왜 이 추천 자료와 학습 경로가 지금 시점에 필요한지 구체적으로 설명하세요.
- next_nodes: 추천 자료와 직접적으로 연결되면서 사용자의 수준({user_level})에 맞는 주제를 제시하세요.
- 반드시 한국어로 작성하세요.\
"""

_DAILY_USER_TEMPLATE = """\
[사용자 정보]
- 현재 수준: {user_level}
- 관심 기술 스택: {favorite_stacks}
- 최근 진행한 커리큘럼 이력: {recent_curricula}

[최근 활동 (최신순)]
{recent_activities}

[기술 숙련도 통계 (많이 사용한 순)]
{skill_stats}

[추천 후보 자료]
{reference_list}

위 정보를 바탕으로, 사용자의 현재 상태를 진단하고 가장 적절한 추천 결과를 생성하세요.\
"""


async def run_daily_recommendation(request: DailyRecommendationRequest) -> DailyRecommendationPayload:
    """POST /api/v1/ai/recommendations/daily-generate 메인 엔트리 포인트."""
    logger.info(
        "[run_daily_recommendation] user_id=%s curriculum_id=%s level=%s",
        request.user_id,
        request.curriculum_id,
        request.current_level.value,
    )

    # 1. Qdrant 검색어 구성 및 검색
    query_text = _build_daily_query(request)
    candidates = _retrieve_from_qdrant_raw(query_text, request.current_level)

    if not candidates:
        logger.warning("[run_daily_recommendation] 후보 자료 없음 — 목 데이터 활용")
        candidates = _load_mock_candidates()

    # 2. 통합 LLM 호출 (한 번의 호출로 6개 필드 모두 생성)
    try:
        output = await _generate_daily_with_llm(request, candidates)
    except Exception as exc:
        logger.warning("[run_daily_recommendation] LLM 실패 (%s) — 템플릿 폴백", exc)
        output = _generate_daily_with_template(request, candidates)

    # 3. Payload 조립
    payload = _assemble_daily_response(request, candidates, output)
    logger.info("[run_daily_recommendation] 성공 — payload 조립 완료")
    return payload


def _build_daily_query(request: DailyRecommendationRequest) -> str:
    """skill_stats 상위 기술과 recent_activities 기술을 조합하여 검색어를 만든다."""
    keywords = []
    # 1. skill_stats 상위 2개
    if request.skill_stats:
        sorted_stats = sorted(request.skill_stats, key=lambda s: s.count, reverse=True)
        keywords.extend([s.skill for s in sorted_stats[:2]])
    
    # 2. 최근 활동에서 등장한 기술 1~2개 추가
    act_skills = []
    for act in request.recent_activities:
        act_skills.extend(act.tech_stacks)
    
    unique_act_skills = list(dict.fromkeys(act_skills))  # 중복 제거
    
    # skill_stats에 없는 활동 기술만 추가
    for skill in unique_act_skills:
        if skill not in keywords:
            keywords.append(skill)
        if len(keywords) >= 4:
            break
            
    # 후보가 전혀 없다면 관심 기술 스택 사용
    if not keywords and request.favorite_tech_stacks:
        keywords.extend(request.favorite_tech_stacks[:3])

    if not keywords:
        return "개발 백엔드 프론트엔드"

    return " ".join(keywords) + " 학습 자료"


def _retrieve_from_qdrant_raw(query_text: str, level: UserLevel) -> list[dict[str, Any]]:
    """문자열 쿼리로 직접 Qdrant를 검색한다."""
    level_hint = _LEVEL_LABEL[level]
    target_query = f"{query_text} {level_hint}"

    try:
        client = get_qdrant_client()
        results = client.query(
            collection_name=QDRANT_COLLECTION_NAME,
            query_text=target_query,
            limit=_MAX_CANDIDATES,
        )
        candidates = [
            {
                "title":          hit.metadata.get("title", ""),
                "url":            hit.metadata.get("url", "") or hit.metadata.get("source_url", ""),
                "source_type":    hit.metadata.get("source_type", "tech_blog"),
                "published_at":   hit.metadata.get("published_at", None),
                "skill_tags":     hit.metadata.get("skill_tags", []) or hit.metadata.get("skill", []),
                "recommendation_reason": "",
                "score":          hit.score,
            }
            for hit in results
            if hit.metadata.get("title")
        ]
        return candidates
    except Exception as exc:
        logger.warning("[_retrieve_from_qdrant_raw] Qdrant 연결 불가 (%s)", exc)
        return []


async def _generate_daily_with_llm(
    request: DailyRecommendationRequest,
    candidates: list[dict[str, Any]],
) -> _DailyRecommendationOutput:
    
    llm = get_model(TaskType.RECOMMENDATION_REASON, temperature=0.3)
    structured_llm = llm.with_structured_output(_DailyRecommendationOutput)

    # 최근 활동 포맷팅
    act_lines = []
    for act in request.recent_activities[:7]:
        stacks = ", ".join(act.tech_stacks) or "없음"
        act_lines.append(f"- [{act.activity_date[:10]}] {act.title} (분류: {act.category}, 기술: {stacks})")
    recent_activities_str = "\n".join(act_lines) if act_lines else "활동 기록 없음"

    # 스킬 통계 포맷팅
    stat_lines = []
    sorted_stats = sorted(request.skill_stats, key=lambda s: s.count, reverse=True)
    for s in sorted_stats[:5]:
        stat_lines.append(f"- {s.skill}: {s.count}회")
    skill_stats_str = "\n".join(stat_lines) if stat_lines else "통계 없음"

    prompt = ChatPromptTemplate.from_messages([
        ("system", _DAILY_SYSTEM_PROMPT),
        ("human",  _DAILY_USER_TEMPLATE),
    ])
    chain = prompt | structured_llm

    result: _DailyRecommendationOutput = await chain.ainvoke({
        "user_level":       _LEVEL_LABEL[request.current_level],
        "favorite_stacks":  ", ".join(request.favorite_tech_stacks) or "미지정",
        "recent_curricula": _fmt_recent_curricula(request.recent_curricula_ids),
        "recent_activities": recent_activities_str,
        "skill_stats":       skill_stats_str,
        "reference_list":   _fmt_candidates(candidates[:5]),
    })
    return result


def _generate_daily_with_template(
    request: DailyRecommendationRequest,
    candidates: list[dict[str, Any]],
) -> _DailyRecommendationOutput:
    
    level = _LEVEL_LABEL[request.current_level]
    top_skill = request.skill_stats[0].skill if request.skill_stats else "주력 기술"
    
    return _DailyRecommendationOutput(
        reason_summary=f"현재 {top_skill} 숙련도와 최근 활동을 고려한 맞춤형 추천입니다.",
        reason_detail=f"최근 {top_skill} 위주의 활동이 잦아, 이를 뒷받침할 {level} 수준의 심화 자료를 선정했습니다.",
        status_summary=f"{top_skill} 중심으로 꾸준히 학습을 진행하고 있습니다.",
        status_detail=f"제공된 활동 기록에 따르면 {top_skill} 관련 경험이 누적되고 있습니다.",
        top_skills=[top_skill],
        next_nodes=[_NextNodeItem(title=f"{top_skill} 성능 최적화")],
    )


def _assemble_daily_response(
    request: DailyRecommendationRequest,
    candidates: list[dict[str, Any]],
    output: _DailyRecommendationOutput,
) -> DailyRecommendationPayload:
    
    references = [
        RecommendationReference(
            title=c["title"],
            recommendation_reason=_item_reason(c, output.reason_summary),
            reference_type=_map_source_type(c.get("source_type", "")),
            published_at=c.get("published_at"),
            url=c.get("url", ""),
        )
        for c in candidates[:5]
        if c.get("title") and c.get("url")
    ]

    next_nodes = [
        RecommendationNextNode(title=n.title)
        for n in output.next_nodes
    ]

    return DailyRecommendationPayload(
        user_id=request.user_id,
        curriculum_id=request.curriculum_id,
        recommendation_reason=RecommendationReason(
            summary=output.reason_summary,
            detail=output.reason_detail,
        ),
        current_status=CurrentStatus(
            summary=output.status_summary,
            detail=output.status_detail,
            top_skills=output.top_skills,
        ),
        references=references,
        next_nodes=next_nodes,
    )
