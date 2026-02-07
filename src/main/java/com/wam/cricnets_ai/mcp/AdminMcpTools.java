package com.wam.cricnets_ai.mcp;

import com.wam.cricnets_ai.model.Role;
import com.wam.cricnets_ai.model.SystemConfig;
import com.wam.cricnets_ai.model.User;
import com.wam.cricnets_ai.model.Booking;
import com.wam.cricnets_ai.repository.BookingRepository;
import com.wam.cricnets_ai.service.BookingService;
import com.wam.cricnets_ai.repository.SystemConfigRepository;
import com.wam.cricnets_ai.repository.UserRepository;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class AdminMcpTools {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final BookingService bookingService;

    public AdminMcpTools(UserRepository userRepository, BookingRepository bookingRepository, SystemConfigRepository systemConfigRepository, BookingService bookingService) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.bookingService = bookingService;
    }

    @McpTool(name = "list_all_users", description = "List all registered users")
    public List<User> listAllUsers() {
        return userRepository.findAll();
    }

    @McpTool(name = "search_users", description = "Search users by name or email")
    public List<User> searchUsers(String query) {
        return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
    }

    @McpTool(name = "toggle_user_status", description = "Enable or disable a user by their ID")
    public User toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }

    @McpTool(name = "update_user_role", description = "Update a user's role (USER, ADMIN, SUPER_ADMIN)")
    public User updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setRole(Role.valueOf(role.toUpperCase()));
        return userRepository.save(user);
    }

    @McpTool(name = "get_dashboard_stats", description = "Get basic statistics for the admin dashboard")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        stats.put("upcomingBookings", bookingRepository.findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime.now()).size());
        return stats;
    }

    @McpTool(name = "get_system_configs", description = "List all system configuration settings")
    public List<SystemConfig> getSystemConfigs() {
        return systemConfigRepository.findAll();
    }

    @McpTool(name = "update_system_config", description = "Update or create a system configuration setting")
    public SystemConfig updateSystemConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElse(new SystemConfig(key, value));
        config.setConfigValue(value);
        return systemConfigRepository.save(config);
    }

    @McpTool(name = "list_all_bookings", description = "List all bookings in the system")
    public List<Booking> listAllBookings() {
        return bookingService.getAllBookings();
    }

    @McpTool(name = "mark_booking_as_done", description = "Mark a booking as completed by its ID")
    public Booking markBookingAsDone(Long bookingId) {
        return bookingService.markAsDone(bookingId);
    }
}
