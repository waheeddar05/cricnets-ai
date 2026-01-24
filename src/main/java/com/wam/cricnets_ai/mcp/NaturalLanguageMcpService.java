package com.wam.cricnets_ai.mcp;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NaturalLanguageMcpService {

    private final ToolRegistry registry;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.ai.google.genai.api-key:AIzaSyC50Q28rhAvM7B5DCBkYWXHEKc6PKwev7U}")
    private String openAiApiKey;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-3-flash-preview}")
    private String LLM_MODEL;

    @Value("${spring.ai.google.genai.chat.options.temperature:0.1}")
    private double TEMPERATURE;

    public NaturalLanguageMcpService(ToolRegistry registry) {
        this.registry = registry;
    }

    public RouteAndResult interpret(String command, boolean execute) {
        // Build a compact tool catalog for the prompt
        List<ToolRegistry.ToolSpec> tools = registry.listTools();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a tool router. Choose the single best tool that satisfies the user's request.\n");
        sb.append("Current date and time: ").append(java.time.LocalDateTime.now()).append("\n");
//        sb.append("Return STRICT JSON only with keys: tool (string), args (object). No extra text.\n");
//        sb.append("If some parameters are unknown, set them to null. Prefer companyId if explicitly provided; otherwise use companyName if given.\n");
//        sb.append("Tools available (name, description, params):\n");
        for (var t : tools) {
            sb.append("- ").append(t.name()).append(": ")
                    .append(t.description() == null ? "" : t.description()).append(" Params: ");
            for (int i = 0; i < t.params().size(); i++) {
                var p = t.params().get(i);
                sb.append(p.name()).append(":").append(p.type());
                if (i < t.params().size() - 1) sb.append(", ");
            }
            sb.append("\n");
        }

        String system = sb.toString();

        String rawJson = callOpenAiForJson(system, command);

        // Parse the model output as JSON
        Map<String, Object> parsed = parseJsonObject(rawJson);
        String tool = (String) parsed.getOrDefault("tool", "");
        Object argsObj = parsed.get("args");
        Map<String, Object> args = argsObj instanceof Map<?, ?> m ? castToStringObjectMap(m) : new HashMap<>();

        RouteAndResult result = new RouteAndResult();
        result.tool = tool;
        result.args = args;
        result.executed = false;

        if (execute && tool != null && !tool.isBlank()) {
            Object execution = registry.callTool(tool, args);
            result.executed = true;
            result.result = execution;
        }
        return result;
    }

    private String callOpenAiForJson(String system, String user) {


        String url = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        Map<String, Object> body = new HashMap<>();
        body.put("model", LLM_MODEL);
        body.put("temperature", TEMPERATURE);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        Map<String, Object> resp = restTemplate.postForObject(url, req, Map.class);
        if (resp == null) throw new IllegalStateException("OpenAI response was null");
        List<?> choices = (List<?>) resp.get("choices");
        if (choices == null || choices.isEmpty()) throw new IllegalStateException("OpenAI returned no choices");
        Map<?, ?> choice0 = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice0.get("message");
        Object content = message != null ? message.get("content") : null;
        if (content == null) throw new IllegalStateException("OpenAI returned empty content");
        return content.toString();
    }

    private Map<String, Object> parseJsonObject(String raw) {
        try {
            return mapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            // If model didn't obey, try to extract a JSON object substring
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String sub = raw.substring(start, end + 1);
                try {
                    return mapper.readValue(sub, new TypeReference<>() {});
                } catch (Exception ignored) { }
            }
            throw new IllegalArgumentException("Model did not return valid JSON: " + raw);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToStringObjectMap(Map<?, ?> map) {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }

    public static class RouteAndResult {
        public String tool;
        public Map<String, Object> args;
        public boolean executed;
        public Object result;
    }
}
