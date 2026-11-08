package com.learning.lpstarter.config;

import com.learning.lpcommon.interceptors.LoginInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class WebConfigTest {

    @Test
    void configuresOnlyExplicitCorsOriginsAndHeaders() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of(" https://learning.example.com ", "https://learning.example.com"));
        WebConfig webConfig = new WebConfig(mock(LoginInterceptor.class), properties);
        ExposedCorsRegistry registry = new ExposedCorsRegistry();

        webConfig.addCorsMappings(registry);

        CorsConfiguration cors = registry.configurations().get("/**");
        assertEquals(List.of("https://learning.example.com"), cors.getAllowedOrigins());
        assertEquals(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"), cors.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"),
                cors.getAllowedHeaders());
        assertEquals(false, cors.getAllowCredentials());
    }

    @Test
    void emptyProductionOriginsDoNotFallBackToWildcard() {
        WebConfig webConfig = new WebConfig(mock(LoginInterceptor.class), new CorsProperties());
        ExposedCorsRegistry registry = new ExposedCorsRegistry();

        webConfig.addCorsMappings(registry);

        CorsConfiguration cors = registry.configurations().get("/**");
        assertEquals(List.of(), cors.getAllowedOrigins());
        assertEquals(false, cors.getAllowCredentials());
    }

    @Test
    void rejectsWildcardOrigins() {
        CorsProperties properties = new CorsProperties();

        assertThrows(IllegalArgumentException.class,
                () -> properties.setAllowedOrigins(List.of("https://*.example.com")));
    }

    private static final class ExposedCorsRegistry extends CorsRegistry {
        private Map<String, CorsConfiguration> configurations() {
            return super.getCorsConfigurations();
        }
    }
}
