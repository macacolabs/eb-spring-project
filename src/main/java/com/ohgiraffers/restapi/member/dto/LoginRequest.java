package com.ohgiraffers.restapi.member.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LoginRequest {

    private String memberId;
    private String memberPassword;
}
