package com.wam.cricnets_ai.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class BookingLock {
    @Id
    private String resourceId;

    public BookingLock() {}

    public BookingLock(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
}
