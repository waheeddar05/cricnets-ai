package com.wam.cricnets_ai.mcp;


import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ToolRegistry {

    private final BookingMcpTools bookingMcpTools;
    private final Map<String, Method> tools = new ConcurrentHashMap<>();
    private final Map<String, ToolSpec> specs = new ConcurrentHashMap<>();

    public ToolRegistry(BookingMcpTools bookingMcpTools) {
        this.bookingMcpTools = bookingMcpTools;
        registerTools(bookingMcpTools.getClass());
    }

    private void registerTools(Class<?> clazz) {
        Class<?> userClass = ClassUtils.getUserClass(clazz);
        for (Method m : userClass.getDeclaredMethods()) {
            McpTool ann = m.getAnnotation(McpTool.class);
            if (ann != null) {
                m.setAccessible(true);
                tools.put(ann.name(), m);
                specs.put(ann.name(), toSpec(ann, m));
            }
        }
    }

    private ToolSpec toSpec(McpTool ann, Method method) {
        List<ParamSpec> params = new ArrayList<>();
        Parameter[] methodParams = method.getParameters();
        for (int i = 0; i < methodParams.length; i++) {
            Parameter p = methodParams[i];
            String name = p.getName();
            if (name.startsWith("arg")) {
                // If we have generic names, try to provide a better one based on common patterns
                // or just rely on the description to guide the AI if we can't do better.
                // However, the best is to just tell the AI to use 'date' if it's a LocalDate etc.
            }
            params.add(new ParamSpec(name, p.getType().getSimpleName()));
        }
        return new ToolSpec(ann.name(), ann.description(), params);
    }

    public List<String> listToolNames() {
        List<String> names = new ArrayList<>(tools.keySet());
        Collections.sort(names);
        return names;
    }

    public List<ToolSpec> listTools() {
        List<ToolSpec> list = new ArrayList<>(specs.values());
        list.sort(Comparator.comparing(ToolSpec::name));
        return list;
    }

    public Object callTool(String toolName, Map<String, Object> args) {
        Method m = tools.get(toolName);
        if (m == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
        try {
            Object[] resolved = resolveArguments(m, args == null ? Map.of() : args);
            return m.invoke(bookingMcpTools, resolved);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Tool execution failed: " + cause.getMessage(), cause);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call tool '" + toolName + "'", e);
        }
    }

    private Object[] resolveArguments(Method m, Map<String, Object> args) {
        Parameter[] params = m.getParameters();
        Object[] resolved = new Object[params.length];
        Map<String, Object> lowerArgs = new HashMap<>();
        args.forEach((k, v) -> lowerArgs.put(k.toLowerCase(Locale.ROOT), v));
        
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            String name = p.getName();
            Class<?> type = p.getType();
            Object raw = lowerArgs.get(name.toLowerCase(Locale.ROOT));

            if (raw == null) {
                // Positional fallback
                List<String> sortedKeys = new ArrayList<>(args.keySet());
                Collections.sort(sortedKeys);
                if (i < sortedKeys.size()) {
                    raw = args.get(sortedKeys.get(i));
                }
            }

            resolved[i] = convert(raw, type);
        }
        return resolved;
    }

    private Object convert(Object raw, Class<?> target) {
        if (raw == null) {
            return null;
        }
        if (target.isInstance(raw)) {
            return raw;
        }
        if (target == String.class) {
            return String.valueOf(raw);
        }
        if ((target == Long.class || target == long.class)) {
            if (raw instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(raw));
        }
        if ((target == Integer.class || target == int.class)) {
            if (raw instanceof Number n) return n.intValue();
            return Integer.parseInt(String.valueOf(raw));
        }
        if ((target == Double.class || target == double.class)) {
            if (raw instanceof Number n) return n.doubleValue();
            return Double.parseDouble(String.valueOf(raw));
        }
        if ((target == Boolean.class || target == boolean.class)) {
            if (raw instanceof Boolean b) return b;
            return Boolean.parseBoolean(String.valueOf(raw));
        }
        if (target == LocalDate.class) {
            return LocalDate.parse(String.valueOf(raw));
        }
        if (target == LocalDateTime.class) {
            try {
                return LocalDateTime.parse(String.valueOf(raw));
            } catch (DateTimeParseException e) {
                // Try LocalDate + start of day if it's just a date
                return LocalDate.parse(String.valueOf(raw)).atStartOfDay();
            }
        }
        if (target.isEnum()) {
            return Enum.valueOf((Class<Enum>) target, String.valueOf(raw));
        }
        // Fallback: return raw and hope for compatible type (e.g., Map -> DTO handled by callee)
        return raw;
    }

    public record ToolSpec(String name, String description, List<ParamSpec> params) {
    }

    public record ParamSpec(String name, String type) {
    }
}