package com.resumeai.jobmatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobMatchOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ResumeAI Job Match Service")
                .description("LinkedIn job fetch, resume-job fit scoring, bookmarking and recommendation APIs")
                .version("v1"));
    }
}