package com.ssafy.s14p21a506.testping.service;

import com.ssafy.s14p21a506.testping.dto.InfrastructureStatusResponse;
import com.ssafy.s14p21a506.testping.dto.TestPingResponse;

public interface TestPingService {

    TestPingResponse pingSpring();

    TestPingResponse pingFastApi();

    InfrastructureStatusResponse checkInfrastructure();
}
