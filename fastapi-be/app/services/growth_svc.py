"""
성장 일지 요약 서비스 — POST /api/v1/ai/growth/summary
"""

from __future__ import annotations

import logging

from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field

from app.core.model_router import TaskType, get_model, get_model_name
from app.models.schemas import GrowthSummaryRequest, GrowthSummaryResponse

logger = logging.getLogger(__name__)

class _GrowthSummaryOutput(BaseModel):
    summary: str = Field(description="성장 보고서 한 줄 요약")

_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
상세한 성장 통계(활발히 사용한 기술, 연속 학습일, 활동량 등)를 분석하여 사용자를 격려하고 현 상태를 요약하는 단 한 문장의 핵심 코멘트를 작성하세요.
절대로 길게 쓰지 마세요. 담백하고 인상적인 한 줄이어야 합니다.
예시: "최근 Spring Boot에 집중하며 7일 연속으로 흔들림 없는 성장을 보여주고 있습니다!"
"""

_USER_TEMPLATE = """\
[성장 통계 요약]
- 탑 기술 스택: {top_tech_stacks}
- 총 활동 건수: {total_count}건
- 최대 연속 활동일: {max_streak}일
- 최근 가장 성장한 기술: {recent_growth}
- 점수 현황: {score_snapshot}

위 데이터를 바탕으로 한 문장의 성장 요약 라인을 작성하세요.\
"""

async def generate_growth_summary(request: GrowthSummaryRequest) -> GrowthSummaryResponse:
    logger.info("[generate_growth_summary] user_id=%s stats=%s", request.user_id, request.stats.total_activity_count)
    
    llm = get_model(TaskType.RECOMMENDATION_REASON, temperature=0.5)
    structured_llm = llm.with_structured_output(_GrowthSummaryOutput)
    
    prompt = ChatPromptTemplate.from_messages([
        ("system", _SYSTEM_PROMPT),
        ("human",  _USER_TEMPLATE),
    ])
    chain = prompt | structured_llm
    
    try:
        result: _GrowthSummaryOutput = await chain.ainvoke({
            "top_tech_stacks": ", ".join(request.stats.top_tech_stacks) or "없음",
            "total_count": request.stats.total_activity_count,
            "max_streak": request.stats.max_streak_days,
            "recent_growth": request.stats.recent_growth_tech or "골고루 학습",
            "score_snapshot": str(request.stats.tech_score_snapshot),
        })
        summary = result.summary
    except Exception as exc:
        logger.warning("[generate_growth_summary] LLM 실패 (%s)", exc)
        tech = request.stats.recent_growth_tech or (request.stats.top_tech_stacks[0] if request.stats.top_tech_stacks else "개발")
        summary = f"최근 {tech} 중심으로 꾸준한 성장을 이어가고 있습니다."
        
    return GrowthSummaryResponse(summary=summary)
