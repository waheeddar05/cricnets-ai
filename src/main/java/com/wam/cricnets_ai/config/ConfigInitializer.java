package com.wam.cricnets_ai.config;

import com.wam.cricnets_ai.model.SystemConfig;
import com.wam.cricnets_ai.repository.SystemConfigRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigInitializer {

    @Bean
    public CommandLineRunner initConfig(SystemConfigRepository repository, BookingConfig bookingConfig) {
        return args -> {
            if (repository.findByConfigKey("slot_duration_minutes").isEmpty()) {
                repository.save(new SystemConfig("slot_duration_minutes", String.valueOf(bookingConfig.getSlotDurationMinutes())));
            }
            if (repository.findByConfigKey("business_hours_start").isEmpty()) {
                repository.save(new SystemConfig("business_hours_start", bookingConfig.getBusinessHours().getStart().toString()));
            }
            if (repository.findByConfigKey("business_hours_end").isEmpty()) {
                repository.save(new SystemConfig("business_hours_end", bookingConfig.getBusinessHours().getEnd().toString()));
            }
        };
    }
}
