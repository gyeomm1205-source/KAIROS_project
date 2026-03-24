package com.ssafy.s14p21a506.testping.api;

import com.ssafy.s14p21a506.exception.ErrorResponse;
import com.ssafy.s14p21a506.testping.dto.TestPingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Test Ping API", description = "Spring/FastAPI ping-pong test API")
public interface TestPingApiDoc {

    @Operation(summary = "Spring ping", description = "Returns pong from Spring Boot")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = TestPingResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<TestPingResponse> pingSpring();

    @Operation(summary = "Spring to FastAPI ping", description = "Spring calls FastAPI ping endpoint")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = TestPingResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "FastAPI unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<TestPingResponse> pingFastApi();
}
