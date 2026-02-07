package com.wam.cricnets_ai.mcp;
 
import com.wam.cricnets_ai.model.*;
import com.wam.cricnets_ai.repository.BookingRepository;
import com.wam.cricnets_ai.repository.SystemConfigRepository;
import com.wam.cricnets_ai.repository.UserRepository;
import com.wam.cricnets_ai.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
 
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ToolRegistryTest {
 
    private BookingService bookingService;
    private BookingMcpTools bookingMcpTools;
    private AdminMcpTools adminMcpTools;
    private UserRepository userRepository;
    private BookingRepository bookingRepository;
    private SystemConfigRepository systemConfigRepository;
    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        bookingService = Mockito.mock(BookingService.class);
        userRepository = Mockito.mock(UserRepository.class);
        bookingRepository = Mockito.mock(BookingRepository.class);
        systemConfigRepository = Mockito.mock(SystemConfigRepository.class);
        
        bookingMcpTools = new BookingMcpTools(bookingService);
        adminMcpTools = new AdminMcpTools(userRepository, bookingRepository, systemConfigRepository, bookingService);
        
        registry = new ToolRegistry(bookingMcpTools, adminMcpTools);
    }

    @Test
    void testListTools() {
        List<ToolRegistry.ToolSpec> tools = registry.listTools();
        
        assertFalse(tools.isEmpty(), "Tools list should not be empty");
        assertTrue(tools.stream().anyMatch(t -> t.name().equals("get_available_slots")), "Should contain get_available_slots");
        assertTrue(tools.stream().anyMatch(t -> t.name().equals("book_session")), "Should contain book_session");
        assertTrue(tools.stream().anyMatch(t -> t.name().equals("list_all_users")), "Should contain list_all_users");
        assertTrue(tools.stream().anyMatch(t -> t.name().equals("get_dashboard_stats")), "Should contain get_dashboard_stats");
    }

    @Test
    void testCallToolWithDateString() {
        LocalDate date = LocalDate.of(2026, 1, 25);
        registry.callTool("get_available_slots", Map.of("date", "2026-01-25", "wicketType", "INDOOR_ASTRO_TURF"));

        Mockito.verify(bookingService).getSlotsForDay(eq(date), eq(WicketType.INDOOR_ASTRO_TURF));
    }

    @Test
    void testCallToolWithGenericArgName() {
        LocalDate date = LocalDate.of(2026, 1, 25);
        // "arg0" is a common default if -parameters is missing
        registry.callTool("get_available_slots", Map.of("arg0", "2026-01-25", "arg1", "INDOOR_ASTRO_TURF"));

        Mockito.verify(bookingService).getSlotsForDay(eq(date), eq(WicketType.INDOOR_ASTRO_TURF));
    }

    @Test
    void testCallAdminTool() {
        registry.callTool("list_all_users", Map.of());
        Mockito.verify(userRepository).findAll();
    }
}
