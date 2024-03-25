package com.leandroruhl.jbalancer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("")
@Slf4j
public class LoadBalancerController {
    @GetMapping()
    public Mono<String> get() {
        log.info("Hello from Load Balancer!");
        return Mono.just("Hello from Load Balancer!");
    }
}
