package com.ssafy.springbootbe.domain.users.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.domain.users.dto.request.DarkModeUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.request.UserProfileUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileUpdateResponse;
import com.ssafy.springbootbe.domain.users.service.UsersService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return ResponseEntity.ok(usersService.findProfile(principal.getUserId()));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<UserProfileUpdateResponse> updateMyProfile(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(usersService.updateProfile(principal.getUserId(), request));
    }

    @PatchMapping("/me/settings/dark-mode")
    public ResponseEntity<Map<String, Boolean>> updateDarkMode(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @RequestBody DarkModeUpdateRequest request) {
        return ResponseEntity.ok(usersService.updateDarkMode(principal.getUserId(), request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        usersService.deleteUser(principal.getUserId(), token);
        return ResponseEntity.noContent().build();
    }
}