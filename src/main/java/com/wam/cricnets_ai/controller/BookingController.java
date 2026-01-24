package com.wam.cricnets_ai.controller;

import com.wam.cricnets_ai.model.BallType;
import com.wam.cricnets_ai.model.Booking;
import com.wam.cricnets_ai.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:3000")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/slots")
    public List<BookingService.SlotStatus> getSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "TENNIS") BallType ballType) {
        return bookingService.getSlotsForDay(date, ballType);
    }

    @PostMapping
    public Booking bookSession(@RequestBody BookingRequest request) {
        return bookingService.createBooking(request.startTime(), request.durationMinutes(), request.ballType(), request.playerName());
    }

    @PostMapping("/multi")
    public List<Booking> bookMultipleSessions(@RequestBody MultiBookingRequest request) {
        return bookingService.createMultiBooking(request.startTimes(), request.ballType(), request.playerName());
    }

    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/{id}")
    public Booking getBooking(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @DeleteMapping("/{id}")
    public void cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
    }

    @GetMapping("/player/{playerName}")
    public List<Booking> getPlayerBookings(@PathVariable String playerName) {
        return bookingService.getBookingsByPlayer(playerName);
    }

    @GetMapping("/upcoming")
    public List<Booking> getUpcomingBookings() {
        return bookingService.getUpcomingBookings();
    }

    public record BookingRequest(LocalDateTime startTime, Integer durationMinutes, BallType ballType, String playerName) {}
    public record MultiBookingRequest(List<LocalDateTime> startTimes, BallType ballType, String playerName) {}
}
