package com.wam.cricnets_ai.mcp;

import com.wam.cricnets_ai.model.BallType;
import com.wam.cricnets_ai.model.Booking;
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

    @McpTool(name = "get_available_slots", description = "Get available cricket net booking slots for a specific date and ball type (TENNIS, LEATHER, TENNIS_MACHINE, LEATHER_MACHINE)")
    public List<BookingService.SlotStatus> getAvailableSlots(LocalDate date, BallType ballType) {
        if (ballType == null) {
            ballType = BallType.TENNIS;
        }
        return bookingService.getSlotsForDay(date, ballType);
    }

    @McpTool(name = "book_session", description = "Book a cricket net session")
    public Booking bookSession(LocalDateTime startTime, Integer durationMinutes, BallType ballType) {
        return bookingService.createBooking(startTime, durationMinutes, ballType, null);
    }

    @McpTool(name = "book_multiple_slots", description = "Book multiple cricket net sessions at once")
    public List<Booking> bookMultipleSlots(List<LocalDateTime> startTimes, BallType ballType) {
        return bookingService.createMultiBooking(startTimes, ballType, null);
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
