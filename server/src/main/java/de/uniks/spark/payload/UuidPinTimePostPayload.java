package de.uniks.spark.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
public class UuidPinTimePostPayload {
    private String uuid;
    private String pin;
    private String timestamp;

    public boolean isValid() {
        return ((uuid != null) && (pin != null)  && (timestamp != null)
                && (uuid.length() == 36) && (pin.length() == 4)
                && LocalDateTime.parse(timestamp).isAfter(LocalDateTime.now().minus(1, ChronoUnit.HOURS))
                && LocalDateTime.parse(timestamp).isBefore(LocalDateTime.now()));
    }
}
