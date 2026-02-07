package com.wam.cricnets_ai.controller;

import com.wam.cricnets_ai.model.*;
import com.wam.cricnets_ai.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/slots")
    public List<BookingService.SlotStatus> getSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "INDOOR_ASTRO_TURF") WicketType wicketType) {
        return bookingService.getSlotsForDay(date, wicketType);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Booking bookSession(@RequestBody BookingRequest request, java.security.Principal principal) {
        String email = principal != null ? principal.getName() : null;
        return bookingService.createBooking(
                request.startTime(),
                request.durationMinutes(),
                request.ballType(),
                request.wicketType(),
                request.machineType(),
                request.leatherBallOption(),
                request.selfOperated(),
                email);
    }

    @PostMapping("/multi")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public List<Booking> bookMultipleSessions(@RequestBody MultiBookingRequest request, java.security.Principal principal) {
        String email = principal != null ? principal.getName() : null;
        return bookingService.createMultiBooking(
                request.startTimes(),
                request.ballType(),
                request.wicketType(),
                request.machineType(),
                request.leatherBallOption(),
                request.selfOperated(),
                email);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Booking getBooking(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
    }

    @PostMapping("/{id}/done")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Booking markAsDone(@PathVariable Long id) {
        return bookingService.markAsDone(id);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public List<Booking> getMyBookings(java.security.Principal principal) {
        return bookingService.getBookingsByEmail(principal.getName());
    }


    @GetMapping("/upcoming")
    public List<Booking> getUpcomingBookings() {
        return bookingService.getUpcomingBookings();
    }

    public record BookingRequest(
            LocalDateTime startTime,
            Integer durationMinutes,
            BallType ballType,
            WicketType wicketType,
            MachineType machineType,
            LeatherBallOption leatherBallOption,
            Boolean selfOperated
    ) {}
    public record MultiBookingRequest(
            List<LocalDateTime> startTimes,
            BallType ballType,
            WicketType wicketType,
            MachineType machineType,
            LeatherBallOption leatherBallOption,
            Boolean selfOperated
    ) {}
}
