package com.wam.cricnets_ai.repository;

import com.wam.cricnets_ai.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b WHERE b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findOverlappingBookings(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT b FROM Booking b WHERE b.startTime >= :dayStart AND b.startTime < :dayEnd")
    List<Booking> findBookingsByDay(@Param("dayStart") LocalDateTime dayStart, @Param("dayEnd") LocalDateTime dayEnd);

    List<Booking> findByPlayerNameIgnoreCase(String playerName);

    List<Booking> findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime now);
}
