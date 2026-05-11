package com.Mengge.finance_tracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearer-jwt";

    @Bean
    public OpenAPI financeTrackerOpenApi() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("Personal Finance Tracker API")
                    .version("1.0")
                    .description(
                        "REST API for income/expense transactions, monthly budgets, and dashboard insights. "
                            + "Authenticate via `/api/auth/login` or `/api/auth/register`, then use `Authorization: Bearer <token>`."
                    )
            )
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
            .components(
                new Components()
                    .addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                            .name(BEARER_AUTH)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            );
    }
}
