package de.uniks.postgres.db.model;

import java.sql.Time;

public class VerificationUser {
    public static final String CLASS = "verificationUser";
    public static final String UUID = "uuid";
    public static final String STATUS = "status";
    public static final String TIMESTAMP = "timestamp";
    public static final String PIN = "pin";

    private String uuid;
    private Boolean status;
    private Time timestamp;
    private String pin;
}
