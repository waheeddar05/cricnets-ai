package com.wam.cricnets_ai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_times", columnList = "startTime, endTime")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BallType ballType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WicketType wicketType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MachineType machineType = MachineType.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeatherBallOption leatherBallOption = LeatherBallOption.NONE;

    private boolean selfOperated = false;

    @Column(nullable = false)
    private String playerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    private String userEmail;

    public Booking() {}

    public Booking(LocalDateTime startTime, LocalDateTime endTime, BallType ballType, WicketType wicketType, MachineType machineType, LeatherBallOption leatherBallOption, boolean selfOperated, String userEmail, String playerName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.ballType = ballType;
        this.wicketType = wicketType;
        this.machineType = machineType;
        this.leatherBallOption = leatherBallOption;
        this.selfOperated = selfOperated;
        this.userEmail = userEmail;
        this.playerName = playerName;
        this.status = BookingStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BallType getBallType() {
        return ballType;
    }

    public void setBallType(BallType ballType) {
        this.ballType = ballType;
    }

    public WicketType getWicketType() {
        return wicketType;
    }

    public void setWicketType(WicketType wicketType) {
        this.wicketType = wicketType;
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public void setMachineType(MachineType machineType) {
        this.machineType = machineType;
    }

    public LeatherBallOption getLeatherBallOption() {
        return leatherBallOption;
    }

    public void setLeatherBallOption(LeatherBallOption leatherBallOption) {
        this.leatherBallOption = leatherBallOption;
    }

    public boolean isSelfOperated() {
        return selfOperated;
    }

    public void setSelfOperated(boolean selfOperated) {
        this.selfOperated = selfOperated;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
