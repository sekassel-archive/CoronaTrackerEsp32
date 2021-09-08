package de.uniks.spark.payload;

import de.uniks.cwa.utils.CWACryptography;
import de.uniks.postgres.db.model.InfectedUser;
import lombok.Data;

import java.time.*;
import java.util.Date;

@Data
public class InfectedUserPostPayload implements Validable {
    private String uuid;
    private String tek;
    private Integer enin;

    // chip collects and send enin (so we now exactly when the tek were created)
    // but we need rsin for infection check and warn everyone before the first contact till 0 o'clock

    public boolean isValid() {
        Integer rsin = getRsin();
        boolean validUuid = ((uuid != null) && (uuid.length() == 36));
        boolean validTek = ((tek != null) && (tek.length() > 31));
        boolean validRsin = ((rsin != null) && ((int) (Math.log10(rsin) + 1) == 7));

        boolean validTimeSpan = (validRsin)
                && (rsin < (CWACryptography.getRollingStartIntervalNumber((System.currentTimeMillis() / 1000L))))
                && (rsin > (CWACryptography.getRollingStartIntervalNumber((System.currentTimeMillis() / 1000L)) - (24 * 144)));

        return validUuid && validTek && validTimeSpan;
    }

    public Integer getRsin() {
        Date date = new Date(new Long(new Integer(enin)) * 600L * 1000L);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        return CWACryptography.getRollingStartIntervalNumber(date.getTime() / 1000L);
    }

    public InfectedUser getInfectedUserForDB() {
        return new InfectedUser(uuid, tek, getRsin());
    }
}
