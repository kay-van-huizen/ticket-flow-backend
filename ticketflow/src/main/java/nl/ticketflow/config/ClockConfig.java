package nl.ticketflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // Same zone as LocalDateTime.now(); tests pass a fixed Clock instead
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
