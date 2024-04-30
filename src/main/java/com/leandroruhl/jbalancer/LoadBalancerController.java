package com.leandroruhl.jbalancer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

@RestController
@Slf4j
public class LoadBalancerController {
    private final WebClient webClient;
    private final ArrayList<Server> servers;
    private final LinkedList<Server> activeServers;
    private final HashSet<Server> activeServersSet;
    private Integer requestCounter;

    public LoadBalancerController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.requestCounter = 0;
        this.servers = new ArrayList<>();
        this.activeServers = new LinkedList<>();
        this.activeServersSet = new HashSet<>();
        Server s1 = new Server("http://localhost:8081", "http://localhost:8081/health");
        Server s2 = new Server("http://localhost:8082", "http://localhost:8082/health");
        servers.add(s1);
        servers.add(s2);
        activeServers.add(s1);
        activeServers.add(s2);
        activeServersSet.add(s1);
        activeServersSet.add(s2);
    }

    @RequestMapping(value = "/**")
    public Mono<String> forwardRequest(ServerHttpRequest request) {
        String backendServerUrl;
        try {
            backendServerUrl = determineBackendServerUrl();
        } catch (IllegalStateException e) {
            return Mono.just(e.getMessage());
        }

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
                })
                .onErrorResume(WebClientRequestException.class, ex -> Mono.just("Server unreachable"));
    }

    private String determineBackendServerUrl() {
        if (activeServers.isEmpty())
            throw new IllegalStateException("No servers available");

        Server server = activeServers.get(requestCounter % activeServers.size());
        log.info("Request counter: " + requestCounter + " Active servers: " + activeServers.size());
        return server.getBaseUrl();
    }

    @Scheduled(fixedDelay = 10000)
    private void healthCheck() {
        Flux.fromIterable(servers)  // Convert the list of servers to a Flux
                .flatMap(server -> {
                    String url = server.getHealthCheckUrl();
                    // Make health check request asynchronously
                    return makeHealthCheckRequest(url)
                            .map(isUp -> new HealthCheckResult(server, isUp));
                })
                .subscribe(healthCheckResult -> {
                    Server server = healthCheckResult.getServer();
                    log.info("Health check result for " + server.getBaseUrl() + " is " + healthCheckResult.isUp());
                    if (healthCheckResult.isUp() && !activeServersSet.contains(server)) {
                        log.info(server.getBaseUrl() + " is being added");
                        activeServers.add(server);
                        activeServersSet.add(server);
                    } else if (!healthCheckResult.isUp() && activeServersSet.contains(server)) {
                        log.warn(server.getBaseUrl() + " is being removed");
                        activeServers.remove(server);
                        activeServersSet.remove(server);
                    }
                });
    }

    private Mono<Boolean> makeHealthCheckRequest(String url) {
        return this.webClient.get()
                .uri(url)
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();
                    return Mono.just(status.is2xxSuccessful());
                })
                .onErrorReturn(false);
    }
}
