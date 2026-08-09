package com.ohgiraffers.restapi.member.controller;

import com.ohgiraffers.restapi.common.ResponseDTO;
import com.ohgiraffers.restapi.member.dto.LoginRequest;
import com.ohgiraffers.restapi.member.dto.MemberDTO;
import com.ohgiraffers.restapi.member.dto.TokenDTO;
import com.ohgiraffers.restapi.member.service.AuthService;
import com.ohgiraffers.restapi.util.ConvertUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ResponseDTO> signup(@RequestBody MemberDTO memberDTO){
        // 멤버의 기본 상태값 설정
        memberDTO.setMemberStatus("Y");
        return ResponseEntity
                .ok()
                .body(new ResponseDTO(HttpStatus.CREATED, "회원가입 성공", authService.signup(memberDTO)));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        TokenDTO tokenDTO = authService.login(request);
        return buildTokenResponse(tokenDTO, "로그인 성공");
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        TokenDTO tokenDTO = authService.refreshToken(refreshToken);
        return buildTokenResponse(tokenDTO, "토큰 재발급 성공");
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDTO> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        ResponseCookie deleteCookie = createDeleteRefreshTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new ResponseDTO(HttpStatus.OK, "로그아웃 성공", null));
    }

    private ResponseEntity<Map<String, Object>> buildTokenResponse(TokenDTO tokenDTO, String message) {
        ResponseCookie cookie = createRefreshTokenCookie(tokenDTO.getRefreshToken());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("userInfo", ConvertUtil.convertObjectToMap(tokenDTO));
        responseMap.put("status", 200);
        responseMap.put("message", message);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(responseMap);
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
    }

    private ResponseCookie createDeleteRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }
}
