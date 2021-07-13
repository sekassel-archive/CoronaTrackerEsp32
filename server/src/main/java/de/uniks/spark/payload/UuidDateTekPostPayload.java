package de.uniks.spark.payload;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UuidDateTekPostPayload {
    private String uuid;
    private String date;
    private String tekList;

    public boolean isValid() {
        return ((uuid != null) && (date != null) && (tekList != null) && (uuid.length() == 36));
    }

    public LocalDateTime getDate() {
        return LocalDateTime.parse(date);
    }
}
