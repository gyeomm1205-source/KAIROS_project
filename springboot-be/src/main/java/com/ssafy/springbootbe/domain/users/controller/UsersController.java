package com.ssafy.springbootbe.domain.users.controller;

import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.domain.users.dto.request.UserProfileUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileUpdateResponse;
import com.ssafy.springbootbe.domain.users.service.UsersService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;
    private final JWTUtils jwtUtils;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @RequestHeader("Authorization") String authorizationHeader) {
        Long userId = extractUserId(authorizationHeader);
        return ResponseEntity.ok(usersService.findProfile(userId));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<UserProfileUpdateResponse> updateMyProfile(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody UserProfileUpdateRequest request) {
        Long userId = extractUserId(authorizationHeader);
        return ResponseEntity.ok(usersService.updateProfile(userId, request));
    }

    private Long extractUserId(String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        Claims claims = jwtUtils.getClaims(token);
        return ((Number) claims.get("userId")).longValue();
    }
}
