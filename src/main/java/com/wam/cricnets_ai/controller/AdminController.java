package com.wam.cricnets_ai.controller;

import com.wam.cricnets_ai.model.Role;
import com.wam.cricnets_ai.model.User;
import com.wam.cricnets_ai.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
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
