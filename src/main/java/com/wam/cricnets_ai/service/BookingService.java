package com.wam.cricnets_ai.service;

import com.wam.cricnets_ai.config.BookingConfig;
import com.wam.cricnets_ai.model.BallType;
import com.wam.cricnets_ai.model.Booking;
import com.wam.cricnets_ai.model.BookingLock;
import com.wam.cricnets_ai.model.SystemConfig;
import com.wam.cricnets_ai.repository.BookingLockRepository;
import com.wam.cricnets_ai.repository.BookingRepository;
import com.wam.cricnets_ai.repository.SystemConfigRepository;
import com.wam.cricnets_ai.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingLockRepository bookingLockRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;
    private final BookingConfig bookingConfig;

    public BookingService(BookingRepository bookingRepository, 
                          BookingLockRepository bookingLockRepository, 
                          SystemConfigRepository systemConfigRepository,
                          UserRepository userRepository,
                          BookingConfig bookingConfig) {
        this.bookingRepository = bookingRepository;
        this.bookingLockRepository = bookingLockRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.userRepository = userRepository;
        this.bookingConfig = bookingConfig;
    }

    private int getSlotDuration() {
        return systemConfigRepository.findByConfigKey("slot_duration_minutes")
                .map(c -> Integer.parseInt(c.getConfigValue()))
                .orElse(bookingConfig.getSlotDurationMinutes());
    }

    private LocalTime getBusinessStart() {
        return systemConfigRepository.findByConfigKey("business_hours_start")
                .map(c -> LocalTime.parse(c.getConfigValue()))
                .orElse(bookingConfig.getBusinessHours().getStart());
    }

    private LocalTime getBusinessEnd() {
        return systemConfigRepository.findByConfigKey("business_hours_end")
                .map(c -> LocalTime.parse(c.getConfigValue()))
                .orElse(bookingConfig.getBusinessHours().getEnd());
    }

    @Transactional
    public Booking createBooking(LocalDateTime startTime, Integer durationMinutes, BallType ballType, String userEmail) {
        int defaultDuration = getSlotDuration();
        if (durationMinutes == null) {
            durationMinutes = defaultDuration;
        }
        
        validateBookingTime(startTime, durationMinutes);
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

        // Separate locks for different machines or general nets
        String lockId = "GENERAL_LOCK";
        if (ballType == BallType.TENNIS_MACHINE) {
            lockId = "TENNIS_MACHINE_LOCK";
        } else if (ballType == BallType.LEATHER_MACHINE) {
            lockId = "LEATHER_MACHINE_LOCK";
        }
        
        final String finalLockId = lockId;
        bookingLockRepository.findByResourceId(finalLockId)
                .orElseGet(() -> {
                    try {
                        return bookingLockRepository.saveAndFlush(new BookingLock(finalLockId));
                    } catch (Exception e) {
                        return bookingLockRepository.findByResourceId(finalLockId).orElseThrow();
                    }
                });

        List<Booking> overlapping = bookingRepository.findOverlappingBookings(startTime, endTime, ballType);
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Session is already booked or unavailable.");
        }

        Booking booking = new Booking(startTime, endTime, ballType, userEmail);
        return bookingRepository.save(booking);
    }

    // Overloaded for backward compatibility or simple cases
    @Transactional
    public Booking createBooking(LocalDateTime startTime, BallType ballType, String userEmail) {
        return createBooking(startTime, getSlotDuration(), ballType, userEmail);
    }

    private void validateBookingTime(LocalDateTime startTime, int durationMinutes) {
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book a session in the past.");
        }
        
        int slotDuration = getSlotDuration();
        if (durationMinutes <= 0 || durationMinutes % slotDuration != 0) {
            throw new IllegalArgumentException("Duration must be a multiple of " + slotDuration + " minutes.");
        }

        LocalTime time = startTime.toLocalTime();
        LocalTime endTime = time.plusMinutes(durationMinutes);
        
        LocalTime businessStart = getBusinessStart();
        LocalTime businessEnd = getBusinessEnd();

        if (time.isBefore(businessStart) || endTime.isAfter(businessEnd)) {
            throw new IllegalArgumentException("Bookings are only available from " + businessStart + " to " + businessEnd + ".");
        }

        int totalMinutesSinceMidnight = startTime.getHour() * 60 + startTime.getMinute();
        if (totalMinutesSinceMidnight % slotDuration != 0) {
            throw new IllegalArgumentException("Bookings must align to " + slotDuration + "-minute boundaries.");
        }
    }

    public List<SlotStatus> getSlotsForDay(LocalDate date, BallType ballType) {
        LocalTime businessStart = getBusinessStart();
        LocalTime businessEnd = getBusinessEnd();
        int slotDuration = getSlotDuration();

        LocalDateTime dayStart = date.atTime(businessStart);
        LocalDateTime dayEnd = date.atTime(businessEnd);
        List<Booking> bookings = bookingRepository.findBookingsByDay(dayStart, dayEnd, ballType);

        List<SlotStatus> slots = new ArrayList<>();
        LocalDateTime current = dayStart;
        LocalDateTime now = LocalDateTime.now();
        while (current.isBefore(dayEnd)) {
            LocalDateTime slotStart = current;
            LocalDateTime slotEnd = current.plusMinutes(slotDuration);
            
            String status;
            boolean available;
            if (slotStart.isBefore(now)) {
                status = "Unavailable";
                available = false;
            } else {
                boolean isBooked = bookings.stream().anyMatch(b -> 
                    b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(slotStart));
                status = isBooked ? "Booked" : "Available";
                available = !isBooked;
            }
            
            slots.add(new SlotStatus(slotStart, status, available));
            current = slotEnd;
        }
        return slots;
    }

    @Transactional
    public List<Booking> createMultiBooking(List<LocalDateTime> startTimes, BallType ballType, String userEmail) {
        if (startTimes == null || startTimes.isEmpty()) {
            return List.of();
        }

        int slotDuration = getSlotDuration();
        List<LocalDateTime> sortedStartTimes = startTimes.stream()
                .sorted()
                .toList();

        List<Booking> createdBookings = new ArrayList<>();
        if (sortedStartTimes.isEmpty()) return createdBookings;

        LocalDateTime currentStart = sortedStartTimes.get(0);
        int currentDuration = slotDuration;

        for (int i = 1; i < sortedStartTimes.size(); i++) {
            LocalDateTime nextStart = sortedStartTimes.get(i);
            if (nextStart.equals(currentStart.plusMinutes(currentDuration))) {
                // Contiguous
                currentDuration += slotDuration;
            } else {
                // Not contiguous, save previous group
                createdBookings.add(createBooking(currentStart, currentDuration, ballType, userEmail));
                // Start new group
                currentStart = nextStart;
                currentDuration = slotDuration;
            }
        }
        // Save the last group
        createdBookings.add(createBooking(currentStart, currentDuration, ballType, userEmail));

        return createdBookings;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @bookingService.isBookingOwner(#id, principal)")
    @Transactional
    public void cancelBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new RuntimeException("Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
    }

    public boolean isBookingOwner(Long id, java.security.Principal principal) {
        if (principal == null) return false;
        return bookingRepository.findById(id)
                .map(b -> principal.getName().equals(b.getUserEmail()))
                .orElse(false);
    }


    public List<Booking> getBookingsByEmail(String email) {
        return bookingRepository.findByUserEmail(email);
    }

    public List<Booking> getUpcomingBookings() {
        return bookingRepository.findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime.now());
    }

    public record SlotStatus(LocalDateTime startTime, String status, boolean available) {}
}
