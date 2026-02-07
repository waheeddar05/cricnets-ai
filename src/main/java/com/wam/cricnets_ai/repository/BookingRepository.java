package com.wam.cricnets_ai.repository;

import com.wam.cricnets_ai.model.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.startTime < :endTime AND b.endTime > :startTime AND b.wicketType = :wicketType AND b.status != 'CANCELLED'")
    List<Booking> findOverlappingBookingsForUpdate(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("wicketType") com.wam.cricnets_ai.model.WicketType wicketType);

    @Query("SELECT b FROM Booking b WHERE b.startTime < :endTime AND b.endTime > :startTime AND b.wicketType = :wicketType AND b.status != 'CANCELLED'")
    List<Booking> findOverlappingBookings(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("wicketType") com.wam.cricnets_ai.model.WicketType wicketType);

    @Query("SELECT b FROM Booking b WHERE b.startTime < :endTime AND b.endTime > :startTime AND b.status != 'CANCELLED'")
    List<Booking> findAllOverlappingBookings(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT b FROM Booking b WHERE b.startTime >= :dayStart AND b.startTime < :dayEnd AND b.wicketType = :wicketType AND b.status != 'CANCELLED'")
    List<Booking> findBookingsByDay(@Param("dayStart") LocalDateTime dayStart, @Param("dayEnd") LocalDateTime dayEnd, @Param("wicketType") com.wam.cricnets_ai.model.WicketType wicketType);


    @Query("SELECT b FROM Booking b WHERE b.userEmail = :email")
    List<Booking> findByUserEmail(@Param("email") String email);

    List<Booking> findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime now);
}
