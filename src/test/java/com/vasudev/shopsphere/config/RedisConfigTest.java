package com.vasudev.shopsphere.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RedisConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class, RedisConfig.class));

    @Test
    @DisplayName("RedisConfig and Redis auto-configuration load into context without error")
    void redisConfigurationLoadsSuccessfully() {
        assertDoesNotThrow(() -> {
            contextRunner.run(context -> {
                assertNotNull(context.getBean(RedisConfig.class));
            });
        });
    }
}
