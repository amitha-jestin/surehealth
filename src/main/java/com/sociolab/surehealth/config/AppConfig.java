package com.sociolab.surehealth.config;

import com.sociolab.surehealth.exception.utils.ProblemDetailFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    @Bean
    public ProblemDetailFactory problemDetailFactory() {
        return new ProblemDetailFactory(
                "https://surehealth/errors/",
                "v1",
                Clock.systemDefaultZone()
        );
    }
}