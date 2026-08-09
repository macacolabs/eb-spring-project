package com.ohgiraffers.restapi.agent.dto;

public record AgentProductRequest(int productCode, String productName, String productPrice,
                                  String productOrderable, String productImageUrl, Long productStock) {
}
