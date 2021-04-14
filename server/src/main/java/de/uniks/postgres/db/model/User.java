package de.uniks.postgres.db.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class User {
    // user is a reserved SQL keyword, avoid naming CLASS just "user" (used as table name)
    public static final String CLASS = "trackerUser";
    public static final String UUID = "uuid";
    public static final String STATUS = "status";
    public static final String ENIN = "enin";
    public static final String RPILIST = "rpiList";

    private String uuid;
    private Integer status;
    private Integer enin;
    private List<byte[]> rpiList;

    public User(String uuid, Integer status, Integer enin) {
        this.uuid = uuid;
        this.status = status;
        this.enin = enin;
    }

    public User(String uuid, Integer status, Integer enin, List<byte[]> rpiList) {
        this(uuid, status, enin);
        this.rpiList = rpiList;
    }

    public User(String uuid, Integer status, Integer enin, String rpiListAsJSONArray) {
        this(uuid, status, enin);
        // Deserialization because of getUserTekListAsJSONArray()
        Type collectionType = new TypeToken<List<byte[]>>() {
        }.getType();
        this.rpiList = new Gson().fromJson(rpiListAsJSONArray, collectionType);
    }

    @Override
    public String toString() {
        return CLASS + "["
                + UUID + "=" + uuid
                + ", " + STATUS + "=" + status.toString()
                + ", " + ENIN + "=" + enin.toString()
                + ", " + RPILIST + "=" + rpiList
                + ']';
    }

    public String getUuid() {
        return uuid;
    }

    public Integer getStatus() {
        return status;
    }

    public Integer getEnin() {
        return enin;
    }

    public String getRpiListAsJSONArray() {
        return new Gson().toJson(rpiList);
    }

    public List<byte[]> getRpiList() {
        return rpiList;
    }
}
