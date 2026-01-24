package com.wam.cricnets_ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_configs")
public class SystemConfig {

    @Id
    private String configKey;

    @Column(nullable = false)
    private String configValue;

    public SystemConfig() {}

    public SystemConfig(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
