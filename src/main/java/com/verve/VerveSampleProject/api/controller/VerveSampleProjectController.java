package com.verve.VerveSampleProject.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/api/verve")
public class VerveSampleProjectController {

    @Autowired
    private RedisTemplate<String, Boolean> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(VerveSampleProjectController.class);
    private final RestTemplate restTemplate = new RestTemplate();

    private int uniqueRequestsPerMinute = 0;

    @GetMapping("/accept")
    public String processRequest(
            @RequestParam(name = "id") Integer id,
            @RequestParam(name = "endpoint", required = false) String httpEndpoint
    ) {
        try {
            String redisKey = id.toString();
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().get(redisKey))) {
                logger.info("Duplicate request with id {}", redisKey);
                return "ok";
            }
            redisTemplate.opsForValue().set(redisKey, true, 60, TimeUnit.SECONDS);
            uniqueRequestsPerMinute++;
            if (httpEndpoint != null && !httpEndpoint.isEmpty()) {
                PostRequestDTO requestBody = new PostRequestDTO(uniqueRequestsPerMinute);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<PostRequestDTO> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.exchange(httpEndpoint, HttpMethod.POST, entity, String.class);
                logger.info("Request to url {} returned Http response {}", httpEndpoint, response.getStatusCode());
            }
            return "ok";

        } catch (Exception e) {
            return "failed";
        }
    }

    @Scheduled(fixedRate = 60000)
    public void logUniqueRequestsCount() {
        logger.info("The number of unique requests in past minute is {}", uniqueRequestsPerMinute);
        uniqueRequestsPerMinute = 0;
    }
}