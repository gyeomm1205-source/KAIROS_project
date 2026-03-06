package com.ssafy.s14p21a506.auth.api;

import com.ssafy.s14p21a506.auth.dto.CurrentUserResponse;
import com.ssafy.s14p21a506.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag(name = "Auth API", description = "AWS Cognito authenticated user test API")
public interface AuthApiDoc {

    @Operation(summary = "Current Cognito user", description = "Returns claims from the validated Cognito access token.")
    @SecurityRequirement(name = "cognitoBearer")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = CurrentUserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<CurrentUserResponse> me(@io.swagger.v3.oas.annotations.Parameter(hidden = true) Jwt jwt);
}
