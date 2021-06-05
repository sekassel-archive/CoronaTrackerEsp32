package de.uniks.postgres.db.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class InfectedUser {
    public static final String CLASS = "infectedUser";
    public static final String UUID = "uuid";
    public static final String TEK = "tek";
    public static final String RSIN = "rsin";

    private String uuid;
    private byte[] tek;
    private Integer rsin;

    public InfectedUser(String uuid, Integer rsin) {
        this.uuid = uuid;
        this.rsin = rsin;
    }

    public InfectedUser(String uuid, String tekAsString, Integer rsin) {
        this(uuid, rsin);
        // Deserialization because of getTekAsJSONArray()
        Type collectionType = new TypeToken<byte[]>() {
        }.getType();
        this.tek = new Gson().fromJson(tekAsString, collectionType);
    }

    @Override
    public String toString() {
        return CLASS + "["
                + UUID + "=" + uuid
                + TEK + "=" + tek // TODO: serialisation
                + ", " + RSIN + "=" + rsin.toString()
                + ']';
    }

    public String getUuid() {
        return uuid;
    }

    public Integer getRsin() {
        return rsin;
    }

    public byte[] getTek() {
        return tek;
    }

    public String getTekAsJSONArray() {
        return new Gson().toJson(getTek());
    }
}
