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

    @McpTool(name = "get_available_slots", description = "Get available cricket net booking slots for a specific date")
    public List<BookingService.SlotStatus> getAvailableSlots(LocalDate date) {
        return bookingService.getSlotsForDay(date);
    }

    @McpTool(name = "book_session", description = "Book a cricket net session")
    public Booking bookSession(LocalDateTime startTime, BallType ballType, String playerName) {
        return bookingService.createBooking(startTime, ballType, playerName);
    }

    @McpTool(name = "get_player_bookings", description = "Get all bookings for a specific player")
    public List<Booking> getPlayerBookings(String playerName) {
        return bookingService.getBookingsByPlayer(playerName);
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
