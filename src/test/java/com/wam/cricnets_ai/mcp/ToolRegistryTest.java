package com.wam.cricnets_ai.mcp;
 
import com.wam.cricnets_ai.model.BallType;
import com.wam.cricnets_ai.service.BookingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
 
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ToolRegistryTest {

    @Test
    void testListTools() {
        BookingService bookingService = Mockito.mock(BookingService.class);
        BookingMcpTools bookingMcpTools = new BookingMcpTools(bookingService);
        ToolRegistry registry = new ToolRegistry(bookingMcpTools);

        List<ToolRegistry.ToolSpec> tools = registry.listTools();
        
        assertFalse(tools.isEmpty(), "Tools list should not be empty");
        assertTrue(tools.stream().anyMatch(t -> t.name().equals("get_available_slots")), "Should contain get_available_slots");
        assertTrue(tools.stream().anyMatch(t -> t.name().equals("book_session")), "Should contain book_session");
    }

    @Test
    void testCallToolWithDateString() {
        BookingService bookingService = Mockito.mock(BookingService.class);
        BookingMcpTools bookingMcpTools = new BookingMcpTools(bookingService);
        ToolRegistry registry = new ToolRegistry(bookingMcpTools);

        LocalDate date = LocalDate.of(2026, 1, 25);
        registry.callTool("get_available_slots", Map.of("date", "2026-01-25", "ballType", "TENNIS"));

        Mockito.verify(bookingService).getSlotsForDay(eq(date), eq(BallType.TENNIS));
    }

    @Test
    void testCallToolWithGenericArgName() {
        BookingService bookingService = Mockito.mock(BookingService.class);
        BookingMcpTools bookingMcpTools = new BookingMcpTools(bookingService);
        ToolRegistry registry = new ToolRegistry(bookingMcpTools);

        LocalDate date = LocalDate.of(2026, 1, 25);
        // "arg0" is a common default if -parameters is missing
        registry.callTool("get_available_slots", Map.of("arg0", "2026-01-25", "arg1", "TENNIS"));

        Mockito.verify(bookingService).getSlotsForDay(eq(date), eq(BallType.TENNIS));
    }
}
