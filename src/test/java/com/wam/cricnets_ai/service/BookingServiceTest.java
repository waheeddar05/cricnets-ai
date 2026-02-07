package com.wam.cricnets_ai.service;

import com.wam.cricnets_ai.config.BookingConfig;
import com.wam.cricnets_ai.model.*;
import com.wam.cricnets_ai.repository.BookingLockRepository;
import com.wam.cricnets_ai.repository.BookingRepository;
import com.wam.cricnets_ai.repository.SystemConfigRepository;
import com.wam.cricnets_ai.repository.UserRepository;
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

    @Mock
    private UserRepository userRepository;

    private BookingService bookingService;
    private BookingConfig bookingConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bookingConfig = new BookingConfig();
        // default 30 min, 7-23 business hours
        bookingService = new BookingService(bookingRepository, bookingLockRepository, systemConfigRepository, userRepository, bookingConfig);
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
        String userEmail = "john@example.com";
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(new com.wam.cricnets_ai.model.User(userEmail, "John", null, com.wam.cricnets_ai.model.Role.USER)));
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // This should now succeed with 60 min duration (default)
        Booking booking = bookingService.createBooking(startTime, null, BallType.TENNIS_MACHINE, userEmail);

        assertNotNull(booking);
        assertEquals(startTime.plusMinutes(60), booking.getEndTime());
        
        // This should fail if we try to book 30 mins because it's not a multiple of 60
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(startTime, 30, BallType.TENNIS_MACHINE, userEmail));
    }

    @Test
    void testCreateBooking_Success() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        String userEmail = "john@example.com";
        String userName = "John Doe";
        com.wam.cricnets_ai.model.User user = new com.wam.cricnets_ai.model.User(userEmail, userName, null, com.wam.cricnets_ai.model.Role.USER);
        
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(startTime, BallType.TENNIS_MACHINE, userEmail);

        assertNotNull(booking);
        assertEquals(userEmail, booking.getUserEmail());
        assertEquals(userName, booking.getPlayerName());
        verify(bookingRepository).save(any());
    }

    @Test
    void testCreateBooking_MultiSlot_Success() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        String userEmail = "john@example.com";
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(new com.wam.cricnets_ai.model.User(userEmail, "John", null, com.wam.cricnets_ai.model.Role.USER)));
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(startTime, 60, BallType.TENNIS_MACHINE, userEmail);

        assertNotNull(booking);
        assertEquals(startTime, booking.getStartTime());
        assertEquals(startTime.plusMinutes(60), booking.getEndTime());
    }

    @Test
    void testCreateBooking_Overlap() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(List.of(new Booking()));

        Exception exception = assertThrows(RuntimeException.class, () -> 
            bookingService.createBooking(startTime, BallType.TENNIS_MACHINE, "jane@example.com"));

        assertEquals("This wicket is already booked for the selected time.", exception.getMessage());
    }

    @Test
    void testCreateBooking_InvalidTime() {
        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime tooEarly = tomorrow.withHour(6).withMinute(30);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(tooEarly, BallType.TENNIS, "early@example.com"));

        LocalDateTime tooLate = tomorrow.withHour(23).withMinute(0);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(tooLate, BallType.TENNIS, "owl@example.com"));

        LocalDateTime invalidSlot = tomorrow.withHour(10).withMinute(15);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(invalidSlot, BallType.TENNIS, "irreg@example.com"));
    }

    @Test
    void testCreateBooking_InvalidDuration() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(startTime, 45, BallType.TENNIS, "inv@example.com"));
    }

    @Test
    void testCreateMultiBooking_Contiguous() {
        LocalDateTime startTime1 = LocalDate.now().plusDays(1).atTime(10, 0);
        LocalDateTime startTime2 = LocalDate.now().plusDays(1).atTime(10, 30);
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Booking> bookings = bookingService.createMultiBooking(List.of(startTime1, startTime2), BallType.TENNIS, "multi@example.com");

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

        List<Booking> bookings = bookingService.createMultiBooking(List.of(startTime1, startTime2), BallType.TENNIS, "multi@example.com");

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
        Booking existing = new Booking(bookedSlot, bookedSlot.plusMinutes(30), BallType.LEATHER, WicketType.INDOOR_ASTRO_TURF, MachineType.NONE, LeatherBallOption.NONE, false, "exist@example.com", "Existing Player");
        
        when(bookingRepository.findBookingsByDay(any(), any(), any())).thenReturn(List.of(existing));

        List<BookingService.SlotStatus> slots = bookingService.getSlotsForDay(date, WicketType.INDOOR_ASTRO_TURF);
        
        // 7 AM to 11 PM = 16 hours = 32 slots
        assertEquals(32, slots.size());
        
        BookingService.SlotStatus tenAM = slots.stream()
                .filter(s -> s.startTime().equals(bookedSlot))
                .findFirst().orElseThrow();
        assertEquals("Booked", tenAM.status());
        assertFalse(tenAM.available());

        BookingService.SlotStatus sevenAM = slots.stream()
                .filter(s -> s.startTime().equals(date.atTime(7, 0)))
                .findFirst().orElseThrow();
        assertEquals("Available", sevenAM.status());
        assertTrue(sevenAM.available());
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
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        
        bookingService.cancelBooking(1L);
        
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void testCancelBooking_NotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> bookingService.cancelBooking(1L));
    }

    @Test
    void testMarkAsDone_Success() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.markAsDone(1L);

        assertEquals(BookingStatus.DONE, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void testCreateBooking_LeatherMachineRequiresBallOption() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            bookingService.createBooking(startTime, 30, BallType.LEATHER, WicketType.INDOOR_ASTRO_TURF, 
                MachineType.LEATHER_BALL_MACHINE, LeatherBallOption.NONE, false, "user@example.com"));

        assertTrue(exception.getMessage().contains("Leather ball machine requires a ball option"));
    }

    @Test
    void testCreateBooking_OperatorLimitReached_TennisMachineSelfOperated() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);
        
        // Mock 2 existing bookings with operators
        Booking b1 = new Booking(startTime, endTime, BallType.LEATHER, WicketType.OUTDOOR_CEMENT, MachineType.LEATHER_BALL_MACHINE, LeatherBallOption.MACHINE_BALL, false, "u1@e.com", "P1");
        Booking b2 = new Booking(startTime, endTime, BallType.LEATHER, WicketType.OUTDOOR_TURF, MachineType.LEATHER_BALL_MACHINE, LeatherBallOption.MACHINE_BALL, false, "u2@e.com", "P2");
        
        when(bookingRepository.findOverlappingBookings(any(), any(), eq(WicketType.INDOOR_ASTRO_TURF))).thenReturn(List.of());
        when(bookingRepository.findAllOverlappingBookings(startTime, endTime)).thenReturn(List.of(b1, b2));
        // Default operator count is 2
        
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.createBooking(startTime, 30, BallType.TENNIS, WicketType.INDOOR_ASTRO_TURF, 
            MachineType.TENNIS_BALL_MACHINE, LeatherBallOption.NONE, false, "user@example.com");

        assertNotNull(result);
        assertTrue(result.isSelfOperated(), "Should automatically switch to self-operated if operators are busy");
    }

    @Test
    void testCreateBooking_OperatorLimitReached_LeatherMachineFails() {
        LocalDateTime startTime = LocalDate.now().plusDays(1).atTime(10, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);
        
        Booking b1 = new Booking(startTime, endTime, BallType.LEATHER, WicketType.OUTDOOR_CEMENT, MachineType.LEATHER_BALL_MACHINE, LeatherBallOption.MACHINE_BALL, false, "u1@e.com", "P1");
        Booking b2 = new Booking(startTime, endTime, BallType.LEATHER, WicketType.OUTDOOR_TURF, MachineType.LEATHER_BALL_MACHINE, LeatherBallOption.MACHINE_BALL, false, "u2@e.com", "P2");
        
        when(bookingRepository.findOverlappingBookings(any(), any(), eq(WicketType.INDOOR_ASTRO_TURF))).thenReturn(List.of());
        when(bookingRepository.findAllOverlappingBookings(startTime, endTime)).thenReturn(List.of(b1, b2));
        
        Exception exception = assertThrows(RuntimeException.class, () -> 
            bookingService.createBooking(startTime, 30, BallType.LEATHER, WicketType.INDOOR_ASTRO_TURF, 
                MachineType.LEATHER_BALL_MACHINE, LeatherBallOption.MACHINE_BALL, false, "user@example.com"));

        assertEquals("No machine operators available for this time slot.", exception.getMessage());
    }

    @Test
    void testGetBookingsByEmail() {
        when(bookingRepository.findByUserEmail("john@example.com")).thenReturn(List.of(new Booking()));
        List<Booking> results = bookingService.getBookingsByEmail("john@example.com");
        assertEquals(1, results.size());
    }
}
