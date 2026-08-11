package com.ohgiraffers.restapi.agent.controller;

import com.ohgiraffers.restapi.agent.service.AgentService;
import com.ohgiraffers.restapi.common.ResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/agent")
@Slf4j
public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ResponseDTO> recommendToday() {
        try {
            return ResponseEntity.ok(new ResponseDTO(HttpStatus.OK, "오늘의 메뉴 추천 조회 성공",
                    agentService.recommendToday()));
        } catch (RestClientException exception) {
            log.error("FastAPI recommendation request failed", exception);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ResponseDTO(HttpStatus.SERVICE_UNAVAILABLE,
                            "메뉴 추천 서비스에 연결할 수 없습니다.", null));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ResponseDTO> health() {
        try {
            return ResponseEntity.ok(new ResponseDTO(HttpStatus.OK, "FastAPI Agent 연결 정상",
                    agentService.health()));
        } catch (RestClientException exception) {
            log.error("FastAPI health request failed", exception);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ResponseDTO(HttpStatus.SERVICE_UNAVAILABLE, "FastAPI Agent 연결 실패", null));
        }
    }
}
