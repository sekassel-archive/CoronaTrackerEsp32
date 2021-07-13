package de.uniks.spark.payload;

import de.uniks.cwa.utils.CWACryptography;
import de.uniks.postgres.db.model.InfectedUser;
import lombok.Data;

@Data
@Deprecated
public class InfectedUserPostPayload implements Validable {
    private String uuid;
    private String tek;
    private Integer rsin; // TODO: change to enin and calculate rsin

    public boolean isValid() {
        boolean validUuid = ((uuid != null) && (uuid.length() == 36));
        boolean validTek = ((tek != null) && (tek.length() > 31));
        boolean validRsin = ((rsin != null) && ((int) (Math.log10(rsin) + 1) == 7));

        boolean validTimeSpan = (validRsin)
                && (rsin < (CWACryptography.getRollingStartIntervalNumber((System.currentTimeMillis() / 1000L))))
                && (rsin > (CWACryptography.getRollingStartIntervalNumber((System.currentTimeMillis() / 1000L)) - (24 * 144)));

        return validUuid && validTek && validTimeSpan;
    }

    public InfectedUser getInfectedUserForDB() {
        return new InfectedUser(uuid, tek, rsin);
    }
}
