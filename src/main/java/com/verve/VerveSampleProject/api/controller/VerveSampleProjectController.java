package com.verve.VerveSampleProject.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


@RestController
@RequestMapping("/api/verve")
public class VerveSampleProjectController {

    private static final Logger logger = LoggerFactory.getLogger(VerveSampleProjectController.class);
    private final RestTemplate restTemplate = new RestTemplate();

    private final ConcurrentMap<Integer, Boolean> incomingUniqueRequests = new ConcurrentHashMap<>();
    private int uniqueRequestsPerMinute = 0;

    @GetMapping("/accept")
    public String processRequest(
            @RequestParam Integer id,
            @RequestParam(required = false) String httpEndpoint
    ) {
        try {
            incomingUniqueRequests.putIfAbsent(id, true);
            if (httpEndpoint != null && !httpEndpoint.isEmpty()) {
                List<Integer> listOfIds = incomingUniqueRequests.keySet().stream().toList();
                PostRequestDTO requestBody = new PostRequestDTO(listOfIds, uniqueRequestsPerMinute);
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
        uniqueRequestsPerMinute = incomingUniqueRequests.size();
        logger.info("The number of unique requests in past minute is {}", uniqueRequestsPerMinute);
        incomingUniqueRequests.clear();
    }
}