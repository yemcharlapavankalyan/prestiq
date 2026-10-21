package org.prestiq.gateway.route;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.client.enabled=false"
})
class RouteMatchingIntegrationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private org.springframework.test.web.reactive.server.WebTestClient webTestClient;

    @Test
    @DisplayName("Should load and configure all 12 explicit service routes")
    void shouldConfigureAllServiceRoutes() {
        Flux<Route> routesFlux = routeLocator.getRoutes();

        StepVerifier.create(routesFlux.collectList())
                .assertNext(routes -> {
                    List<String> routeIds = routes.stream().map(Route::getId).toList();

                    assertThat(routeIds).contains(
                            "auth-service",
                            "student-service",
                            "scholarship-service-public",
                            "scholarship-service-admin",
                            "eligibility-service",
                            "eligibility-service-admin",
                            "recommendation-service",
                            "application-service",
                            "saved-scholarships-service",
                            "notification-service",
                            "ai-service",
                            "ingestion-service"
                    );
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Actuator health check endpoint should return UP status")
    void shouldReturnHealthyActuator() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("Should block HTTP requests targeting internal service endpoints with RFC 7807 403")
    void shouldBlockInternalEndpointsViaHttp() {
        webTestClient.get()
                .uri("/internal/v1/students/00000000-0000-0000-0000-000000000001/profile")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith("application/problem+json")
                .expectBody()
                .jsonPath("$.code").isEqualTo("ERR_INTERNAL_ROUTE_PROHIBITED")
                .jsonPath("$.status").isEqualTo(403);
    }
}

