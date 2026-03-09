package com.ssafy.s14p21a506.mockpost.api;

import com.ssafy.s14p21a506.exception.ErrorResponse;
import com.ssafy.s14p21a506.mockpost.dto.MockPostCreateRequest;
import com.ssafy.s14p21a506.mockpost.dto.MockPostResponse;
import com.ssafy.s14p21a506.mockpost.dto.MockPostUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag(name = "MockPost API", description = "Board CRUD test API")
public interface MockPostApiDoc {

    @Operation(summary = "Create mock post")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Created",
                    content = @Content(schema = @Schema(implementation = MockPostResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "cognitoBearer")
    ResponseEntity<MockPostResponse> create(
            @Parameter(hidden = true) Jwt jwt,
            MockPostCreateRequest request
    );

    @Operation(summary = "List mock posts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "cognitoBearer")
    ResponseEntity<List<MockPostResponse>> list();

    @Operation(summary = "Get mock post")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = MockPostResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "cognitoBearer")
    ResponseEntity<MockPostResponse> get(@Parameter(example = "1") long mockPostId);

    @Operation(summary = "Update mock post")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Updated",
                    content = @Content(schema = @Schema(implementation = MockPostResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "cognitoBearer")
    ResponseEntity<MockPostResponse> update(
            @Parameter(hidden = true) Jwt jwt,
            long mockPostId,
            MockPostUpdateRequest request
    );

    @Operation(summary = "Delete mock post")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "cognitoBearer")
    ResponseEntity<Void> delete(@Parameter(hidden = true) Jwt jwt, long mockPostId);
}
