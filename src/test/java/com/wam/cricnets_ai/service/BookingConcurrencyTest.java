package com.wam.cricnets_ai.service;

import com.wam.cricnets_ai.model.BallType;
import com.wam.cricnets_ai.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("local")
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private com.wam.cricnets_ai.repository.BookingLockRepository bookingLockRepository;

    @Test
    void testConcurrentBookings() throws InterruptedException {
        bookingLockRepository.deleteAll();
        bookingRepository.deleteAll();
        
        // Pre-create the lock to avoid race on insert
        bookingLockRepository.saveAndFlush(new com.wam.cricnets_ai.model.BookingLock("GENERAL_LOCK"));

        LocalDateTime startTime = LocalDate.now().plusYears(1).atTime(14, 0);
        int threads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    latch.await();
                    bookingService.createBooking(startTime, BallType.LEATHER, "player" + index + "@example.com");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(1, successCount.get(), "Only one booking should succeed");
        assertEquals(threads - 1, failureCount.get(), "All other bookings should fail");
        
        // Cleanup
        bookingRepository.deleteAll();
    }
}
