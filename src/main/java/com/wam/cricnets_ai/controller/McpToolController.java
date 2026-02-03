package com.wam.cricnets_ai.controller;


import com.wam.cricnets_ai.mcp.ToolRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/mcp-client", produces = MediaType.APPLICATION_JSON_VALUE)
public class McpToolController {

    private final ToolRegistry invokerService;

    public McpToolController(ToolRegistry invokerService) {
        this.invokerService = invokerService;
    }

    @GetMapping("/tools")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<String>> listTools() {
        return ResponseEntity.ok(invokerService.listToolNames());
    }

    @PostMapping("/tools/{name}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Object> callTool(@PathVariable("name") String name,
                                           @RequestBody(required = false) Map<String, Object> args) {
        Object result = invokerService.callTool(name, args == null ? Map.of() : args);
        return ResponseEntity.ok(result);
    }
}
