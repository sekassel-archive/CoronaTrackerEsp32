package de.uniks.payload;

import lombok.Data;

@Data
public class InfectionPostPayload {
    private static final int AUTH_DATA = 1234;

    private int rsin;
    private byte[] keyData;
    private int authData;

    public boolean isAuthenticated() {
        return authData == AUTH_DATA;
    }

    public boolean isValid() {
        boolean isEpochTime = (int) (Math.log10(rsin) + 1) == 7; //Valid Length
        boolean validTime = rsin < ((System.currentTimeMillis() / 1000L)/ (60 * 10)); //Valid ENIntervalNumber
        return rsin > 0 && keyData != null && keyData.length == 16 && isEpochTime && validTime;
    }
}