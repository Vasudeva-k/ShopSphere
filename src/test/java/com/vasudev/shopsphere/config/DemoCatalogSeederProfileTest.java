package com.vasudev.shopsphere.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vasudev.shopsphere.repository.CategoryRepository;
import com.vasudev.shopsphere.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DemoCatalogSeederProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CategoryRepository.class, () -> Mockito.mock(CategoryRepository.class))
            .withBean(ProductRepository.class, () -> Mockito.mock(ProductRepository.class))
            .withUserConfiguration(DemoCatalogSeeder.class);

    @Test
    @DisplayName("DemoCatalogSeeder bean should NOT be loaded when 'prod' profile is active")
    void seederBean_notLoadedInProdProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertFalse(context.containsBean("demoCatalogSeeder"),
                            "DemoCatalogSeeder must not be loaded when prod profile is active");
                });
    }

    @Test
    @DisplayName("DemoCatalogSeeder bean should NOT be loaded when default profile is active")
    void seederBean_notLoadedInDefaultProfile() {
        contextRunner
                .run(context -> {
                    assertFalse(context.containsBean("demoCatalogSeeder"),
                            "DemoCatalogSeeder must not be loaded when no profile is active");
                });
    }

    @Test
    @DisplayName("DemoCatalogSeeder bean SHOULD be loaded when 'local' profile is active")
    void seederBean_loadedInLocalProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertTrue(context.containsBean("demoCatalogSeeder"),
                            "DemoCatalogSeeder should be loaded when local profile is active");
                });
    }

    @Test
    @DisplayName("DemoCatalogSeeder bean SHOULD be loaded when 'dev' or 'demo' profile is active")
    void seederBean_loadedInDevOrDemoProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> {
                    assertTrue(context.containsBean("demoCatalogSeeder"),
                            "DemoCatalogSeeder should be loaded when dev profile is active");
                });

        contextRunner
                .withPropertyValues("spring.profiles.active=demo")
                .run(context -> {
                    assertTrue(context.containsBean("demoCatalogSeeder"),
                            "DemoCatalogSeeder should be loaded when demo profile is active");
                });
    }
}
