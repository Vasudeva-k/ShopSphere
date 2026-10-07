package com.vasudev.shopsphere.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ActuatorConfigurationTest {

    private Properties prodProperties;

    @BeforeEach
    void setUp() throws IOException {
        prodProperties = new Properties();
        ClassPathResource resource = new ClassPathResource("application-prod.properties");
        try (InputStream inputStream = resource.getInputStream()) {
            prodProperties.load(inputStream);
        }
    }

    @Test
    @DisplayName("Verify management.endpoints.web.exposure.include exposes only health and info")
    void verifyWebExposureIncludesOnlyHealthAndInfo() {
        String exposedEndpoints = prodProperties.getProperty("management.endpoints.web.exposure.include");
        assertNotNull(exposedEndpoints, "management.endpoints.web.exposure.include property must be defined");
        assertEquals("health,info", exposedEndpoints);
    }

    @Test
    @DisplayName("Verify management.endpoint.health.show-details is set to never")
    void verifyHealthShowDetailsIsNever() {
        String showDetails = prodProperties.getProperty("management.endpoint.health.show-details");
        assertNotNull(showDetails, "management.endpoint.health.show-details property must be defined");
        assertEquals("never", showDetails);
    }

    @Test
    @DisplayName("Verify management.info.env.enabled is disabled (false)")
    void verifyInfoEnvIsEnabledFalse() {
        String infoEnvEnabled = prodProperties.getProperty("management.info.env.enabled");
        assertNotNull(infoEnvEnabled, "management.info.env.enabled property must be defined");
        assertEquals("false", infoEnvEnabled);
    }
}
