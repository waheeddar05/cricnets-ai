package com.wam.cricnets_ai.service;

import com.wam.cricnets_ai.model.BallType;
import com.wam.cricnets_ai.model.Booking;
import com.wam.cricnets_ai.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Booking createBooking(LocalDateTime startTime, BallType ballType, String playerName) {
        validateBookingTime(startTime);
        LocalDateTime endTime = startTime.plusMinutes(30);

        List<Booking> overlapping = bookingRepository.findOverlappingBookings(startTime, endTime);
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Session is already booked or unavailable.");
        }

        Booking booking = new Booking(startTime, endTime, ballType, playerName);
        return bookingRepository.save(booking);
    }

    private void validateBookingTime(LocalDateTime startTime) {
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book a session in the past.");
        }
        LocalTime time = startTime.toLocalTime();
        if (time.isBefore(LocalTime.of(7, 0)) || time.isAfter(LocalTime.of(22, 30))) {
            throw new IllegalArgumentException("Bookings are only available from 7:00 AM to 11:00 PM.");
        }
        if (startTime.getMinute() != 0 && startTime.getMinute() != 30) {
            throw new IllegalArgumentException("Bookings must be in 30-minute slots (e.g., 7:00 or 7:30).");
        }
    }

    public List<SlotStatus> getSlotsForDay(LocalDate date) {
        LocalDateTime dayStart = date.atTime(7, 0);
        LocalDateTime dayEnd = date.atTime(23, 0);
        List<Booking> bookings = bookingRepository.findBookingsByDay(dayStart, dayEnd);

        List<SlotStatus> slots = new ArrayList<>();
        LocalDateTime current = dayStart;
        LocalDateTime now = LocalDateTime.now();
        while (current.isBefore(dayEnd)) {
            LocalDateTime slotStart = current;
            LocalDateTime slotEnd = current.plusMinutes(30);
            
            String status;
            if (slotStart.isBefore(now)) {
                status = "Unavailable";
            } else {
                boolean isBooked = bookings.stream().anyMatch(b -> 
                    b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(slotStart));
                status = isBooked ? "Booked" : "Available";
            }
            
            slots.add(new SlotStatus(slotStart, status));
            current = slotEnd;
        }
        return slots;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    public void cancelBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new RuntimeException("Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
    }

    public List<Booking> getBookingsByPlayer(String playerName) {
        return bookingRepository.findByPlayerNameIgnoreCase(playerName);
    }

    public List<Booking> getUpcomingBookings() {
        return bookingRepository.findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime.now());
    }

    public record SlotStatus(LocalDateTime startTime, String status) {}
}
