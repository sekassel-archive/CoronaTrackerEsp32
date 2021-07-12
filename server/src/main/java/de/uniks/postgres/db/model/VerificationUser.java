package de.uniks.postgres.db.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VerificationUser {
    public static final String CLASS = "verificationUser";
    public static final String UUID = "uuid";
    public static final String PIN = "pin";
    public static final String VALID = "expired";
    public static final String INFECTED_STATUS = "infectedStatus";
    public static final String TIMESTAMP = "timestamp";

    private String uuid;
    private String pin;
    private Boolean valid;
    private Boolean infectedStatus;
    private LocalDateTime timestamp;
}
