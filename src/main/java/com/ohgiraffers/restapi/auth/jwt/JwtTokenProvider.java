package com.ohgiraffers.restapi.auth.jwt;

import com.ohgiraffers.restapi.member.dto.MemberDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jwt.key}")
    private String jwtSecret;

    @Value("${jwt.time}")
    private long jwtExpiration;

    @Value("${jwt.refresh-time:604800000}")
    private long jwtRefreshExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(MemberDTO member) {
        return createToken(member, jwtExpiration);
    }

    public String createRefreshToken(MemberDTO member) {
        return createToken(member, jwtRefreshExpiration);
    }

    public long getRefreshExpiration() {
        return jwtRefreshExpiration;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            throw new BadCredentialsException("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            throw new BadCredentialsException("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            throw new BadCredentialsException("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("JWT Token claims empty", e);
        } catch (JwtException e) {
            throw new BadCredentialsException("JWT Token error", e);
        }
    }

    public String getMemberIdFromJWT(String token) {
        return getClaims(token).getSubject();
    }

    private String createToken(MemberDTO member, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(createClaims(member))
                .subject(member.getMemberId())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Map<String, Object> createClaims(MemberDTO member) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("memberName", member.getMemberName());
        claims.put("memberEmail", member.getMemberEmail());
        claims.put("memberRole", member.getMemberRole());
        claims.put("role", extractPrimaryAuthority(member));
        return claims;
    }

    private String extractPrimaryAuthority(MemberDTO member) {
        return member.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");
    }
}
