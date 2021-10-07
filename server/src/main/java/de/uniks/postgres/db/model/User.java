package de.uniks.postgres.db.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Data
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

    public List<String> getRpiListAsJSONArray() {
        List<String> rpiListCollector = new ArrayList<>();
        rpiList.forEach(entry -> {
            rpiListCollector.add("[" + new Gson().toJson(entry) + "]");
        });
        return rpiListCollector;
    }

}
