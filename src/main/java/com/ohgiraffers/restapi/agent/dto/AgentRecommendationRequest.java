package com.ohgiraffers.restapi.agent.dto;

import java.util.List;

public record AgentRecommendationRequest(List<AgentProductRequest> products) {
}
