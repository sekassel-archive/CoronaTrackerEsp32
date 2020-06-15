package de.uniks.payload;

import lombok.Data;

@Data
public class InfectionPostPayload {
    private long time;
    private int id;

    public boolean isValid() {
        boolean isEpochTime = (int) (Math.log10(time) + 1) == 10; //Checks whether time is unix time in seconds
        boolean validTime = time < (System.currentTimeMillis() / 1000L); //Logged time can't be greater than current time
        return time > 0 && id > 0 && isEpochTime && validTime;
    }
}
