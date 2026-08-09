package com.ohgiraffers.restapi.agent.dto;

public record AgentHealthResponse(String status, String service, String version) {
}
