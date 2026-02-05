package com.wam.cricnets_ai.controller;

import com.wam.cricnets_ai.model.Role;
import com.wam.cricnets_ai.model.User;
import com.wam.cricnets_ai.repository.BookingRepository;
import com.wam.cricnets_ai.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public AdminController(UserRepository userRepository, BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/users/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<User> searchUsers(@RequestParam String query) {
        return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        stats.put("upcomingBookings", bookingRepository.findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime.now()).size());
        return stats;
    }

    @PostMapping("/users/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public User updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User user = userRepository.findById(id).orElseThrow();
        user.setRole(Role.valueOf(request.get("role")));
        return userRepository.save(user);
    }

    @PostMapping("/users/{id}/toggle-status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public User toggleUserStatus(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public User inviteAdmin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setRole(Role.ADMIN);
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    User newUser = new User(email, "Invited Admin", null, Role.ADMIN);
                    return userRepository.save(newUser);
                });
    }
}
