package com.wam.cricnets_ai.mcp;

import com.wam.cricnets_ai.model.*;
import com.wam.cricnets_ai.service.BookingService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class BookingMcpTools {

    private final BookingService bookingService;

    public BookingMcpTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @McpTool(name = "get_available_slots", description = "Get available cricket net booking slots for a specific date and wicket type (INDOOR_ASTRO_TURF, OUTDOOR_CEMENT, OUTDOOR_TURF)")
    public List<BookingService.SlotStatus> getAvailableSlots(LocalDate date, WicketType wicketType) {
        if (wicketType == null) {
            wicketType = WicketType.INDOOR_ASTRO_TURF;
        }
        return bookingService.getSlotsForDay(date, wicketType);
    }

    @McpTool(name = "book_session", description = "Book a cricket net session")
    public Booking bookSession(LocalDateTime startTime, Integer durationMinutes, BallType ballType,
                               WicketType wicketType, MachineType machineType, LeatherBallOption leatherBallOption,
                               Boolean selfOperated, String email) {
        return bookingService.createBooking(startTime, durationMinutes, ballType, wicketType, machineType, leatherBallOption, selfOperated, email);
    }

    @McpTool(name = "book_multiple_slots", description = "Book multiple cricket net sessions at once")
    public List<Booking> bookMultipleSlots(List<LocalDateTime> startTimes, BallType ballType, String email) {
        return bookingService.createMultiBooking(startTimes, ballType, email);
    }

    @McpTool(name = "get_user_bookings", description = "Get all bookings for a specific user email")
    public List<Booking> getUserBookings(String email) {
        return bookingService.getBookingsByEmail(email);
    }

    @McpTool(name = "cancel_booking", description = "Cancel an existing cricket net booking by ID")
    public String cancelBooking(Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return "Booking " + bookingId + " cancelled successfully.";
    }

    @McpTool(name = "get_upcoming_bookings", description = "Get all upcoming cricket net bookings")
    public List<Booking> getUpcomingBookings() {
        return bookingService.getUpcomingBookings();
    }
}
