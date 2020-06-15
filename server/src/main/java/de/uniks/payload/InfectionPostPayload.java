package de.uniks.payload;

import lombok.Data;

@Data
public class InfectionPostPayload {
    private long time;
    private int id;

    public boolean isValid() {
        return time > 0 && (int) (Math.log10(time)+1) == 10 && id > 0;
    }
}
