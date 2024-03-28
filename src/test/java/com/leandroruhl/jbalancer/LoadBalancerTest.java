package com.leandroruhl.jbalancer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoadBalancerTest {

    @LocalServerPort
    private Integer port;

    @Container
    private static final GenericContainer<?> backend1 = new GenericContainer<>(DockerImageName.parse("backend-image:latest"))
            .withEnv("SERVER_PORT", "8081")
            .withExposedPorts(8081);

    @Container
    private static final GenericContainer<?> backend2 = new GenericContainer<>(DockerImageName.parse("backend-image:latest"))
            .withEnv("SERVER_PORT", "8082")
            .withExposedPorts(8082);

    @BeforeAll
    static void beforeAll() {
        backend1.start();
        backend2.start();
    }

    @AfterAll
    static void afterAll() {
        backend1.stop();
        backend2.stop();
    }

    @Test
    public void testLoadBalancer() {
        RestTemplate restTemplate = new RestTemplate();

        String backend1Url = "http://" + backend1.getHost() + ":" + backend1.getMappedPort(8081) + "/api/hello";
        String response1 = restTemplate.getForObject(backend1Url, String.class);
        assertEquals("Hello from Backend Server 2!", response1); // Adjust this as per your backend response

        String backend2Url = "http://" + backend2.getHost() + ":" + backend2.getMappedPort(8082) + "/api/hello";
        String response2 = restTemplate.getForObject(backend2Url, String.class);
        assertEquals("Hello from Backend Server 2!", response2); // Adjust this as per your backend response
    }
}

