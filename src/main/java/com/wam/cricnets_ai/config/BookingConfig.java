package com.wam.cricnets_ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Configuration
@ConfigurationProperties(prefix = "booking")
public class BookingConfig {

    private int slotDurationMinutes = 30;
    private int operatorCount = 2;
    private BusinessHours businessHours = new BusinessHours();

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public int getOperatorCount() {
        return operatorCount;
    }

    public void setOperatorCount(int operatorCount) {
        this.operatorCount = operatorCount;
    }

    public BusinessHours getBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(BusinessHours businessHours) {
        this.businessHours = businessHours;
    }

    public static class BusinessHours {
        private LocalTime start = LocalTime.of(7, 0);
        private LocalTime end = LocalTime.of(23, 0);

        public LocalTime getStart() {
            return start;
        }

        public void setStart(LocalTime start) {
            this.start = start;
        }

        public LocalTime getEnd() {
            return end;
        }

        public void setEnd(LocalTime end) {
            this.end = end;
        }
    }
}
