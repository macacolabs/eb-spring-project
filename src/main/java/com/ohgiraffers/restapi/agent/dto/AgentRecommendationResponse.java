package com.ohgiraffers.restapi.agent.dto;

public record AgentRecommendationResponse(int productCode, String productName, String productPrice,
                                          String productImageUrl, String message, String reason,
                                          String servedBy, String apiVersion) {
}
