package de.uniks.db.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class User {
    public static final String USER_CLASS = "user";
    public static final String USER_UUID = "uuid";
    public static final String USER_STATUS = "status";
    public static final String USER_RSIN = "rsin";
    public static final String USER_TEKLIST = "tekList";

    private String uuid;
    private Integer status;
    private Integer rsin;
    private List<byte[]> tekList;

    public User(String uuid, Integer status, Integer rsin, String tekListAsJSONArray) {
        this.uuid = uuid;
        this.status = status;
        this.rsin = rsin;
        // Deserialization because of getUserTekListAsJSONArray()
        Type collectionType = new TypeToken<List<byte[]>>() {
        }.getType();
        this.tekList = new Gson().fromJson(tekListAsJSONArray, collectionType);
    }

    @Override
    public String toString() {
        return USER_CLASS + "["
                + USER_UUID + "=" + uuid
                + ", " + USER_STATUS + "=" + status.toString()
                + ", " + USER_RSIN + "=" + rsin.toString()
                + ", " + USER_TEKLIST + "=" + tekList
                + ']';
    }

    public String getUserUuid() {
        return uuid;
    }

    public Integer getUserStatus() {
        return status;
    }

    public Integer getUserRsin() {
        return rsin;
    }

    public String getUserTekListAsJSONArray() {
        return new Gson().toJson(tekList);
    }
}
