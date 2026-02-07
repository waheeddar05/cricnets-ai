package com.wam.cricnets_ai.controller;


import com.wam.cricnets_ai.mcp.NaturalLanguageMcpService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/mcp-client", produces = MediaType.APPLICATION_JSON_VALUE)
public class McpNaturalLanguageController {

    private final NaturalLanguageMcpService nlService;

    public McpNaturalLanguageController(NaturalLanguageMcpService nlService) {
        this.nlService = nlService;
    }

    public record InterpretRequest(String command, Boolean execute) {}

    @PostMapping("/interpret")
    public ResponseEntity<?> interpret(@RequestBody InterpretRequest request) {
        if (request == null || request.command == null || request.command.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'command'"));
        }
        boolean execute = request.execute == null || Boolean.TRUE.equals(request.execute);
        var result = nlService.interpret(request.command, execute);
        return ResponseEntity.ok(result);
    }
}
