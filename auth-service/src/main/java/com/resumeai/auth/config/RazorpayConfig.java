package com.resumeai.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RazorpayConfig {

    @Bean
    public RestClient razorpayRestClient(@Value("${razorpay.base-url}") String baseUrl,
                                         @Value("${razorpay.key.id}") String keyId,
                                         @Value("${razorpay.key.secret}") String keySecret) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(keyId, keySecret))
                .build();
    }
}
