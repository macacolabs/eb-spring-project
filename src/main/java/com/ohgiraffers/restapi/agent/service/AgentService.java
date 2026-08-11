package com.ohgiraffers.restapi.agent.service;

import com.ohgiraffers.restapi.agent.dto.AgentHealthResponse;
import com.ohgiraffers.restapi.agent.dto.AgentProductRequest;
import com.ohgiraffers.restapi.agent.dto.AgentRecommendationResponse;
import com.ohgiraffers.restapi.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class AgentService {
    private final ProductRepository productRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String agentBaseUrl;

    public AgentService(ProductRepository productRepository, ObjectMapper objectMapper,
                        @Value("${agent.base-url:http://localhost:8000}") String agentBaseUrl) {
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.agentBaseUrl = agentBaseUrl;
        this.restClient = RestClient.builder().baseUrl(agentBaseUrl).build();
    }

    public AgentRecommendationResponse recommendToday() {
        List<AgentProductRequest> products = productRepository.findByProductOrderable("Y").stream()
                .map(product -> new AgentProductRequest(product.getProductCode(), product.getProductName(),
                        product.getProductPrice(), product.getProductOrderable(), product.getProductImageUrl(),
                        product.getProductStock()))
                .toList();

        List<Map<String, Object>> productPayloads = products.stream()
                .map(product -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("productCode", product.productCode());
                    payload.put("productName", product.productName());
                    payload.put("productPrice", product.productPrice());
                    payload.put("productOrderable", product.productOrderable());
                    payload.put("productImageUrl", product.productImageUrl());
                    payload.put("productStock", product.productStock());
                    return payload;
                })
                .toList();
        Map<String, Object> request = Map.of("products", productPayloads);
        byte[] requestBody;
        try {
            requestBody = objectMapper.writeValueAsBytes(request);
        } catch (JacksonException exception) {
            throw new RestClientException("추천 요청을 JSON으로 변환할 수 없습니다.", exception);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(agentBaseUrl + "/recommendations"))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RestClientException("FastAPI가 " + response.statusCode()
                        + " 상태를 반환했습니다: " + response.body());
            }
            return objectMapper.readValue(response.body(), AgentRecommendationResponse.class);
        } catch (JacksonException | IOException exception) {
            throw new RestClientException("추천 API 통신 또는 JSON 변환에 실패했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RestClientException("추천 API 호출이 중단되었습니다.", exception);
        }
    }

    public AgentHealthResponse health() {
        return restClient.get().uri("/health")
                .retrieve().body(AgentHealthResponse.class);
    }
}
