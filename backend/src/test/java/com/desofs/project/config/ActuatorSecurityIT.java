package com.desofs.project.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ActuatorSecurityIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalManagementPort
    private int managementPort;

    @Test
    void livenessEndpoint_IsPublic() throws Exception {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + managementPort + "/actuator/health/liveness", Map.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    void readinessEndpoint_IsPublic() throws Exception {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + managementPort + "/actuator/health/readiness", Map.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    void nonHealthActuatorEndpoint_RemainsProtected() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + managementPort + "/actuator/env", String.class);

        assertEquals(404, response.getStatusCode().value());
    }
}
