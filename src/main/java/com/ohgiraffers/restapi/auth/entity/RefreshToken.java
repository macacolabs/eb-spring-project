package com.ohgiraffers.restapi.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "TBL_REFRESH_TOKEN")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @Column(name = "MEMBER_ID", length = 100)
    private String memberId;

    @Column(name = "TOKEN", length = 1000, nullable = false)
    private String token;

    @Column(name = "EXPIRY_DATE", nullable = false)
    private Date expiryDate;
}
