package com.leandroruhl.jbalancer;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoadBalancerTest {
    @LocalServerPort
    private int port;
    @Container
    private static final GenericContainer<?> backend1 = new GenericContainer<>(DockerImageName.parse("backend-image:latest"))
            .withEnv("SERVER_PORT", "8081")
            .withExposedPorts(8081)
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(8081), new ExposedPort(8081)))
            ));

    @Container
    private static final GenericContainer<?> backend2 = new GenericContainer<>(DockerImageName.parse("backend-image:latest"))
            .withEnv("SERVER_PORT", "8082")
            .withExposedPorts(8082)
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(8082), new ExposedPort(8082)))
            ));

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
    public void testBackendInstances() {
        RestTemplate restTemplate = new RestTemplate();

        String backend1Url = "http://" + backend1.getHost() + ":" + backend1.getMappedPort(8081) + "/api/hello";
        String response1 = restTemplate.getForObject(backend1Url, String.class);
        assertEquals("Hello from server running on port 8081!", response1);

        String backend2Url = "http://" + backend2.getHost() + ":" + backend2.getMappedPort(8082) + "/api/hello";
        String response2 = restTemplate.getForObject(backend2Url, String.class);
        assertEquals("Hello from server running on port 8082!", response2);
    }

    @Test
    public void testLoadBalancerHelloEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        String loadBalancerUrl = "http://localhost:" + port + "/api/hello";
        String response = restTemplate.getForObject(loadBalancerUrl, String.class);
        assertTrue(response.startsWith("Hello from server running on port"));
    }

    @Test
    public void testLoadBalancerPostEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        String requestBody = "Test request body";
        String loadBalancerUrl = "http://localhost:" + port + "/api/users";
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(loadBalancerUrl, requestBody, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("User successfully created!", responseEntity.getBody());
    }

    @Test
    public void testLoadDistribution() {
        RestTemplate restTemplate = new RestTemplate();
        String loadBalancerUrl = "http://localhost:" + port + "/api/hello";
        String response1 = restTemplate.getForObject(loadBalancerUrl, String.class);
        assertTrue(response1.startsWith("Hello from server running on port"));
        String response2 = restTemplate.getForObject(loadBalancerUrl, String.class);
        assertTrue(response2.startsWith("Hello from server running on port"));
        assertNotEquals(response1, response2); // ports should be different
    }
}

