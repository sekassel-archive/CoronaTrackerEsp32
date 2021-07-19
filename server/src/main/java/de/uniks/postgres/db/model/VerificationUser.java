package de.uniks.postgres.db.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * PIN: The correctness of the pin is not checked if value is given.
 *      UserVerificationService will pull the pin for the given uuid/username
 *      and springboot crm compares pin internally.
 *
 * TOKEN_ACTIVE: Prevent more than one active session/entry in db to login with.
 *
 * TOKEN_VALID: This boolean has nothing to do if the given pin was correct or not.
 *              It does just indicate if there was just one active login entry in db and
 *              at this time pin was set.
 */

@Data
public class VerificationUser {
    public static final String CLASS = "verificationUser";
    public static final String UUID = "uuid";
    public static final String PIN = "pin";
    public static final String TOKEN_ACTIVE = "tokenActive";
    public static final String TOKEN_VALID = "tokenValid";
    public static final String TIMESTAMP = "timestamp";

    public static final String INPUT_READY_FOR_PICKUP = "inputReadyForPickup";
    public static final String USER_INFECTED = "userInfected";
    public static final String RSIN = "rsin";

    private String uuid;
    private String pin;
    private Boolean tokenActive;
    private Boolean tokenValid;
    private LocalDateTime timestamp;

    // only set if verification is successful (data comes from user input ui)
    private boolean inputReadyForPickup;
    private boolean userInfected;
    private int rsin;
}
