package com.wam.cricnets_ai.controller;

import com.wam.cricnets_ai.model.SystemConfig;
import com.wam.cricnets_ai.repository.SystemConfigRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
public class ConfigController {

    private final SystemConfigRepository systemConfigRepository;

    public ConfigController(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public SystemConfig updateConfig(@RequestBody Map<String, String> request) {
        String key = request.get("key");
        String value = request.get("value");
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElse(new SystemConfig(key, value));
        config.setConfigValue(value);
        return systemConfigRepository.save(config);
    }
}
