package de.uniks.spark.payload;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.uniks.cwa.utils.CWACryptography;
import de.uniks.postgres.db.model.User;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.List;

@Data
public class UserPostPayload implements Validable {
    private String uuid;
    private Integer status;
    private Integer enin;
    private String rpiList;

    public boolean isValid() {
        boolean validUuid = ((uuid != null) && (uuid.length() == 36));
        boolean validStatus = ((status != null) && (status >= 0) && (status <= 2));
        boolean validEnin = ((enin != null) && ((int) (Math.log10(enin) + 1) == 7));

        boolean validTimeSpan = (validEnin)
                && (enin < (CWACryptography.getRollingStartIntervalNumber((System.currentTimeMillis() / 1000L))))
                && (enin > (CWACryptography.getRollingStartIntervalNumber((System.currentTimeMillis() / 1000L)) - (24 * 144)));
        boolean validRawRpiList = ((rpiList != null) && (rpiList.length() > 31));
        boolean validRpiList = false;

        if (validRawRpiList) {
            Type collectionType = new TypeToken<List<byte[]>>() {
            }.getType();
            List<byte[]> rpiListParsed = new Gson().fromJson(rpiList, collectionType);
            validRpiList = (rpiListParsed != null) && (!rpiListParsed.isEmpty())
                    && rpiListParsed.stream().filter(rpi -> rpi.length != 16).findAny().isEmpty();
        }

        return validUuid && validStatus && validTimeSpan && validRpiList;
    }

    public User getUserForDB() {
        return new User(uuid, status, enin, rpiList);
    }
}
