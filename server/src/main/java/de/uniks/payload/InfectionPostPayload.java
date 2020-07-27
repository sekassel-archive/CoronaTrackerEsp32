package de.uniks.payload;

import lombok.Data;

import java.math.BigInteger;

@Data
public class InfectionPostPayload {
    private static final int AUTH_DATA = 1234;

    private long time;
    private BigInteger id;
    private int authData;

    public boolean isAuthenticated() {
        return authData == AUTH_DATA;
    }

    public boolean isValid() {
        boolean isEpochTime = (int) (Math.log10(time) + 1) == 10; //Checks whether time is unix time in seconds
        boolean validTime = time < (System.currentTimeMillis() / 1000L); //Logged time can't be greater than current time
        return time > 0 && id.compareTo(BigInteger.valueOf(0)) != 0 && isEpochTime && validTime;
    }
}
