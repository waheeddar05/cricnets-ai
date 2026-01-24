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
    @Query("SELECT b FROM Booking b WHERE b.startTime < :endTime AND b.endTime > :startTime AND " +
           "((:ballType IN (com.wam.cricnets_ai.model.BallType.TENNIS_MACHINE, com.wam.cricnets_ai.model.BallType.LEATHER_MACHINE) AND b.ballType = :ballType) OR " +
           "(:ballType NOT IN (com.wam.cricnets_ai.model.BallType.TENNIS_MACHINE, com.wam.cricnets_ai.model.BallType.LEATHER_MACHINE) AND b.ballType NOT IN (com.wam.cricnets_ai.model.BallType.TENNIS_MACHINE, com.wam.cricnets_ai.model.BallType.LEATHER_MACHINE)))")
    List<Booking> findOverlappingBookingsForUpdate(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("ballType") com.wam.cricnets_ai.model.BallType ballType);

    @Query("SELECT b FROM Booking b WHERE b.startTime < :endTime AND b.endTime > :startTime AND " +
           "((:ballType IN (com.wam.cricnets_ai.model.BallType.TENNIS_MACHINE, com.wam.cricnets_ai.model.BallType.LEATHER_MACHINE) AND b.ballType = :ballType) OR " +
           "(:ballType NOT IN (com.wam.cricnets_ai.model.BallType.TENNIS_MACHINE, com.wam.cricnets_ai.model.BallType.LEATHER_MACHINE) AND b.ballType NOT IN (com.wam.cricnets_ai.model.BallType.TENNIS_MACHINE, com.wam.cricnets_ai.model.BallType.LEATHER_MACHINE)))")
    List<Booking> findOverlappingBookings(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("ballType") com.wam.cricnets_ai.model.BallType ballType);

    @Query("SELECT b FROM Booking b WHERE b.startTime >= :dayStart AND b.startTime < :dayEnd AND " +
           "((:ballType IN (com.wam.cricnets_ai.model.BallType.TENNIS_MACHINE, com.wam.cricnets_ai.model.BallType.LEATHER_MACHINE) AND b.ballType = :ballType) OR " +
           "(:ballType NOT IN (com.wam.cricnets_ai.model.BallType.TENNIS_MACHINE, com.wam.cricnets_ai.model.BallType.LEATHER_MACHINE) AND b.ballType NOT IN (com.wam.cricnets_ai.model.BallType.TENNIS_MACHINE, com.wam.cricnets_ai.model.BallType.LEATHER_MACHINE)))")
    List<Booking> findBookingsByDay(@Param("dayStart") LocalDateTime dayStart, @Param("dayEnd") LocalDateTime dayEnd, @Param("ballType") com.wam.cricnets_ai.model.BallType ballType);

    List<Booking> findByPlayerNameIgnoreCase(String playerName);

    List<Booking> findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime now);
}
