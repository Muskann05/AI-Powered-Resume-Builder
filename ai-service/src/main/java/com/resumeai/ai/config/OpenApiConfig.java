package com.resumeai.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ResumeAI AI Content Service API")
                        .version("1.0")
                        .description("AI generation, ATS, quota and history service"));
    }
}
