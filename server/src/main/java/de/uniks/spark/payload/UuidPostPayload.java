package de.uniks.spark.payload;

import lombok.Data;

@Data
public class UuidPostPayload {
    private String uuid;

    public boolean isValid() {
        return ((uuid != null) && (uuid.length() == 36));
    }
}
