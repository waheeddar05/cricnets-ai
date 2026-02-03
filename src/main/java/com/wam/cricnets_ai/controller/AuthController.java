package com.wam.cricnets_ai.controller;

import com.wam.cricnets_ai.config.JwtService;
import com.wam.cricnets_ai.model.Role;
import com.wam.cricnets_ai.model.User;
import com.wam.cricnets_ai.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private static final String SUPER_ADMIN_EMAIL = "waheeddar8@gmail.com";

    public AuthController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/google-login")
    public Map<String, String> googleLogin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String name = request.get("name");
        String picture = request.get("picture");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role role = email.equals(SUPER_ADMIN_EMAIL) ? Role.SUPER_ADMIN : Role.USER;
            User newUser = new User(email, name, picture, role);
            return userRepository.save(newUser);
        });

        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return Map.of("token", token, "role", user.getRole().name());
    }
}
