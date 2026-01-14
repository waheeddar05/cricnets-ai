package com.wam.cricnets_ai.service;

import com.wam.cricnets_ai.model.BallType;
import com.wam.cricnets_ai.model.Booking;
import com.wam.cricnets_ai.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bookingService = new BookingService(bookingRepository);
    }

    @Test
    void testCreateBooking_Success() {
        LocalDateTime startTime = LocalDateTime.of(2026, 1, 15, 10, 0);
        when(bookingRepository.findOverlappingBookings(any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(startTime, BallType.MACHINE, "John Doe");

        assertNotNull(booking);
        assertEquals("John Doe", booking.getPlayerName());
        verify(bookingRepository).save(any());
    }

    @Test
    void testCreateBooking_Overlap() {
        LocalDateTime startTime = LocalDateTime.of(2026, 1, 15, 10, 0);
        when(bookingRepository.findOverlappingBookings(any(), any())).thenReturn(List.of(new Booking()));

        Exception exception = assertThrows(RuntimeException.class, () -> 
            bookingService.createBooking(startTime, BallType.MACHINE, "Jane Doe"));

        assertEquals("Session is already booked or unavailable.", exception.getMessage());
    }

    @Test
    void testCreateBooking_InvalidTime() {
        LocalDateTime tooEarly = LocalDateTime.of(2026, 1, 15, 6, 30);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(tooEarly, BallType.TENNIS, "Early Bird"));

        LocalDateTime tooLate = LocalDateTime.of(2026, 1, 15, 23, 0);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(tooLate, BallType.TENNIS, "Night Owl"));

        LocalDateTime invalidSlot = LocalDateTime.of(2026, 1, 15, 10, 15);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(invalidSlot, BallType.TENNIS, "Irregular"));
    }

    @Test
    void testGetSlotsForDay() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        LocalDateTime bookedSlot = date.atTime(10, 0);
        Booking existing = new Booking(bookedSlot, bookedSlot.plusMinutes(30), BallType.LEATHER, "Existing");
        
        when(bookingRepository.findBookingsByDay(any(), any())).thenReturn(List.of(existing));

        List<BookingService.SlotStatus> slots = bookingService.getSlotsForDay(date);

        // 7 AM to 11 PM = 16 hours = 32 slots
        assertEquals(32, slots.size());
        
        BookingService.SlotStatus tenAM = slots.stream()
                .filter(s -> s.startTime().equals(bookedSlot))
                .findFirst().orElseThrow();
        assertEquals("Booked", tenAM.status());

        BookingService.SlotStatus sevenAM = slots.stream()
                .filter(s -> s.startTime().equals(date.atTime(7, 0)))
                .findFirst().orElseThrow();
        assertEquals("Available", sevenAM.status());
    }
}
