package de.uniks.spark.payload;

import lombok.Data;

@Data
public class UuidPinPostPayload implements Validable {
    private String uuid;
    private String pin;

    public boolean isValid() {
        return ((uuid != null) && (pin != null) && (pin.length() == 4) && (uuid.length() == 36));
    }
}
