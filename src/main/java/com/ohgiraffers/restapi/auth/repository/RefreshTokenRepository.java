package com.ohgiraffers.restapi.auth.repository;

import com.ohgiraffers.restapi.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
}
