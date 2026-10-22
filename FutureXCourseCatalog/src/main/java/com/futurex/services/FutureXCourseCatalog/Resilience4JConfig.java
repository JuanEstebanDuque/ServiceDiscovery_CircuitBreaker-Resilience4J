package com.futurex.services.FutureXCourseCatalog;

import java.time.Duration;

import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

@Configuration
public class Resilience4JConfig {
    @Bean 
    public Customizer<Resilience4JCircuitBreakerFactory> globalCustomConfiguration() { 
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom() 
            .failureRateThreshold(50) 
            .waitDurationInOpenState(Duration.ofMillis(1000)) 
            .slidingWindowSize(2) 
            .build(); 
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id) 
            .circuitBreakerConfig(circuitBreakerConfig) 
            .build()); 
    }
}