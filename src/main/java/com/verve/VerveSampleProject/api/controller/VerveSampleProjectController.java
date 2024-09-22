package com.verve.VerveSampleProject.api.controller;

import com.verve.VerveSampleProject.api.KafkaMessagePublisher;
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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/api/verve")
public class VerveSampleProjectController {

    @Autowired
    private RedisTemplate<String, Boolean> redisTemplate;

    @Autowired
    private KafkaMessagePublisher kafkaMessagePublisher;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Logger logger = LoggerFactory.getLogger(VerveSampleProjectController.class);

    private final ConcurrentMap<Integer, Boolean> incomingUniqueRequests = new ConcurrentHashMap<>();

    @GetMapping("/accept")
    public String processRequest(
            @RequestParam(name = "id") Integer id,
            @RequestParam(name = "endpoint", required = false) String httpEndpoint
    ) {
        try {
            String redisKey = id.toString();
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().get(redisKey))) {
                logger.info("Duplicate request with id {}", redisKey);
            } else {
                redisTemplate.opsForValue().set(redisKey, true, 60, TimeUnit.SECONDS);
                incomingUniqueRequests.putIfAbsent(id, true);
            }

            if (httpEndpoint != null && !httpEndpoint.isEmpty()) {
                List<Integer> listOfIds = incomingUniqueRequests.keySet().stream().toList();
                PostRequestDTO requestBody = new PostRequestDTO(listOfIds, incomingUniqueRequests.size());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<PostRequestDTO> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.exchange(httpEndpoint, HttpMethod.POST, entity, String.class);
                logger.info("Request to url {} returned Http response {}", httpEndpoint, response.getStatusCode());
            }
            return "ok";

        } catch (Exception e) {
            logger.error("The request processing failed due to error: {}", e.getMessage());
            return "failed";
        }
    }

    @Scheduled(fixedRate = 60000)
    public void logUniqueRequestsCount() {
        logger.info("The number of unique requests in past minute is {}", incomingUniqueRequests.size());
        kafkaMessagePublisher.publishMessage("count: " + incomingUniqueRequests.size());
        incomingUniqueRequests.clear();
    }
}