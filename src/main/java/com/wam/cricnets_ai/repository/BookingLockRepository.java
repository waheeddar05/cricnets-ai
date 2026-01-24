package com.wam.cricnets_ai.repository;

import com.wam.cricnets_ai.model.BookingLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface BookingLockRepository extends JpaRepository<BookingLock, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT bl FROM BookingLock bl WHERE bl.resourceId = :resourceId")
    Optional<BookingLock> findByResourceId(@org.springframework.data.repository.query.Param("resourceId") String resourceId);
}
