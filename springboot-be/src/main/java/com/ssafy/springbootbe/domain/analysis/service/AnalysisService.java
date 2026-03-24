package com.ssafy.springbootbe.domain.analysis.service;

import com.ssafy.springbootbe.domain.analysis.dto.request.AnalysisCompleteRequest;
import com.ssafy.springbootbe.domain.analysis.dto.response.AnalysisCompleteResponse;

public interface AnalysisService {

    AnalysisCompleteResponse complete(AnalysisCompleteRequest request);
}
