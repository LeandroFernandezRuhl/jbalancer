package com.leandroruhl.jbalancer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@RestController
@Slf4j
public class LoadBalancerController {
    private final WebClient webClient;
    private final ArrayList<String> serverUrls;
    private Integer requestCounter;

    public LoadBalancerController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.requestCounter = 0;
        this.serverUrls = new ArrayList<>();
        serverUrls.add("http://localhost:8081");
        serverUrls.add("http://localhost:8082");
    }

    @RequestMapping(value = "/**")
    public Mono<String> forwardRequest(ServerHttpRequest request) {
        String backendServerUrl = determineBackendServerUrl();
        this.requestCounter++;

        String uri = UriComponentsBuilder.fromHttpUrl(backendServerUrl)
                .path(request.getPath().value())
                .query(request.getURI().getQuery())
                .build()
                .toUriString();

        return webClient.method(request.getMethod())
                .uri(uri)
                .headers(headers -> headers.putAll(request.getHeaders()))
                .body(BodyInserters.fromDataBuffers(request.getBody()))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().is4xxClientError()) {
                        return Mono.just("Error: " + ex.getStatusCode().value() + " " + ex.getStatusText());
                    } else {
                        return Mono.error(ex);
                    }
                });
    }


    private String determineBackendServerUrl() {
        return serverUrls.get(requestCounter % serverUrls.size());
    }
}
