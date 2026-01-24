package com.wam.cricnets_ai.service;

import com.wam.cricnets_ai.config.BookingConfig;
import com.wam.cricnets_ai.model.BallType;
import com.wam.cricnets_ai.model.Booking;
import com.wam.cricnets_ai.model.BookingLock;
import com.wam.cricnets_ai.model.SystemConfig;
import com.wam.cricnets_ai.repository.BookingLockRepository;
import com.wam.cricnets_ai.repository.BookingRepository;
import com.wam.cricnets_ai.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingLockRepository bookingLockRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    private BookingService bookingService;
    private BookingConfig bookingConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bookingConfig = new BookingConfig();
        // default 30 min, 7-23 business hours
        bookingService = new BookingService(bookingRepository, bookingLockRepository, systemConfigRepository, bookingConfig);
        when(bookingLockRepository.findByResourceId(any())).thenReturn(Optional.of(new BookingLock("GENERAL_LOCK")));
        
        // Mock empty system config by default
        when(systemConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void testCreateBooking_FromDatabaseConfig() {
        // Change slot duration to 60 minutes in DB
        when(systemConfigRepository.findByConfigKey("slot_duration_minutes"))
                .thenReturn(Optional.of(new SystemConfig("slot_duration_minutes", "60")));
        
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // This should now succeed with 60 min duration (default)
        Booking booking = bookingService.createBooking(startTime, null, BallType.TENNIS_MACHINE, "John DB");

        assertNotNull(booking);
        assertEquals(startTime.plusMinutes(60), booking.getEndTime());
        
        // This should fail if we try to book 30 mins because it's not a multiple of 60
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(startTime, 30, BallType.TENNIS_MACHINE, "Should Fail"));
    }

    @Test
    void testCreateBooking_Success() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(startTime, BallType.TENNIS_MACHINE, "John Doe");

        assertNotNull(booking);
        assertEquals("John Doe", booking.getPlayerName());
        verify(bookingRepository).save(any());
    }

    @Test
    void testCreateBooking_MultiSlot_Success() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(startTime, 60, BallType.TENNIS_MACHINE, "John Doe");

        assertNotNull(booking);
        assertEquals(startTime, booking.getStartTime());
        assertEquals(startTime.plusMinutes(60), booking.getEndTime());
    }

    @Test
    void testCreateBooking_Overlap() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(List.of(new Booking()));

        Exception exception = assertThrows(RuntimeException.class, () -> 
            bookingService.createBooking(startTime, BallType.TENNIS_MACHINE, "Jane Doe"));

        assertEquals("Session is already booked or unavailable.", exception.getMessage());
    }

    @Test
    void testCreateBooking_InvalidTime() {
        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime tooEarly = tomorrow.withHour(6).withMinute(30);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(tooEarly, BallType.TENNIS, "Early Bird"));

        LocalDateTime tooLate = tomorrow.withHour(23).withMinute(0);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(tooLate, BallType.TENNIS, "Night Owl"));

        LocalDateTime invalidSlot = tomorrow.withHour(10).withMinute(15);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(invalidSlot, BallType.TENNIS, "Irregular"));
    }

    @Test
    void testCreateBooking_InvalidDuration() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(startTime, 45, BallType.TENNIS, "Invalid Duration"));
    }

    @Test
    void testCreateMultiBooking_Contiguous() {
        LocalDateTime startTime1 = LocalDate.now().plusDays(1).atTime(10, 0);
        LocalDateTime startTime2 = LocalDate.now().plusDays(1).atTime(10, 30);
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Booking> bookings = bookingService.createMultiBooking(List.of(startTime1, startTime2), BallType.TENNIS, "Multi Player");

        assertEquals(1, bookings.size());
        assertEquals(startTime1, bookings.get(0).getStartTime());
        assertEquals(startTime1.plusMinutes(60), bookings.get(0).getEndTime());
    }

    @Test
    void testCreateMultiBooking_NonContiguous() {
        LocalDateTime startTime1 = LocalDate.now().plusDays(1).atTime(10, 0);
        LocalDateTime startTime2 = LocalDate.now().plusDays(1).atTime(12, 0);
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Booking> bookings = bookingService.createMultiBooking(List.of(startTime1, startTime2), BallType.TENNIS, "Multi Player");

        assertEquals(2, bookings.size());
        assertEquals(startTime1, bookings.get(0).getStartTime());
        assertEquals(startTime1.plusMinutes(30), bookings.get(0).getEndTime());
        assertEquals(startTime2, bookings.get(1).getStartTime());
        assertEquals(startTime2.plusMinutes(30), bookings.get(1).getEndTime());
    }

    @Test
    void testGetSlotsForDay() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalDateTime bookedSlot = date.atTime(10, 0);
        Booking existing = new Booking(bookedSlot, bookedSlot.plusMinutes(30), BallType.LEATHER, "Existing");
        
        when(bookingRepository.findBookingsByDay(any(), any(), any())).thenReturn(List.of(existing));

        List<BookingService.SlotStatus> slots = bookingService.getSlotsForDay(date, BallType.LEATHER);
        
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

    @Test
    void testGetAllBookings() {
        when(bookingRepository.findAll()).thenReturn(List.of(new Booking(), new Booking()));
        List<Booking> bookings = bookingService.getAllBookings();
        assertEquals(2, bookings.size());
    }

    @Test
    void testGetBookingById_Found() {
        Booking booking = new Booking();
        booking.setId(1L);
        when(bookingRepository.findById(1L)).thenReturn(java.util.Optional.of(booking));
        Booking result = bookingService.getBookingById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetBookingById_NotFound() {
        when(bookingRepository.findById(1L)).thenReturn(java.util.Optional.empty());
        assertThrows(RuntimeException.class, () -> bookingService.getBookingById(1L));
    }

    @Test
    void testCancelBooking_Success() {
        when(bookingRepository.existsById(1L)).thenReturn(true);
        bookingService.cancelBooking(1L);
        verify(bookingRepository).deleteById(1L);
    }

    @Test
    void testCancelBooking_NotFound() {
        when(bookingRepository.existsById(1L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> bookingService.cancelBooking(1L));
    }

    @Test
    void testGetBookingsByPlayer() {
        when(bookingRepository.findByPlayerNameIgnoreCase("John")).thenReturn(List.of(new Booking()));
        List<Booking> results = bookingService.getBookingsByPlayer("John");
        assertEquals(1, results.size());
    }
}
