package de.uniks.cwa;

import de.uniks.cwa.utils.CWACryptography;
import de.uniks.cwa.utils.CWARequests;
import de.uniks.postgres.db.UserPostgreSqlDao;
import de.uniks.postgres.db.model.User;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CwaDataInterpreter {

    private static final Logger LOG = Logger.getLogger(CwaDataInterpreter.class.getName());
    private static ConcurrentHashMap<Integer, List<byte[]>> cwaData = new ConcurrentHashMap<>();

    public static void ckeckForInfectionsHourlyTask() {
        try {
            // CWA DB get every 1h a new data set. So we'll get a hourly update of cwaData in memory
            cwaData = CWARequests.getUnzippedInfectionData();

            // subsequently, we compare contact information from cwa DB with our postgres DB
            UserPostgreSqlDao dbData = new UserPostgreSqlDao();
            processInfectionCheck(dbData.getAll()); // get the whole postgres db entry's

            LOG.log(Level.INFO, "Hourly CWA data update and check successfully processed at " +
                    DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                            .replaceAll("[TZ]", " ") + " UTC");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "CWA Data couldn't process hourly update during getData from CWA Database!", ex);
        }
    }

    /**
     * This part should compare the data set of the cwa data set and the collected data from our user db.
     *
     */
    private static void processInfectionCheck(Collection<User> userCollection) {
        //TODO: check Infections update / status
        List<User> infectedUser = new ArrayList();
        // gotta check for each user if cwa impacts our user db entry's
        userCollection.parallelStream()
                .filter(user -> !user.getRpiList().isEmpty())
                .filter(user -> cwaData.containsKey(user.getEnin()))
                .filter(user -> user.getStatus().equals(0))
                .forEach(user -> {
                    Integer rsin = user.getEnin();
                    List<byte[]> cwaTekList = cwaData.get(rsin);
                    cwaTekList.parallelStream().forEach(infectedTek -> {
                        for (int rollingTimeOffset = 0; rollingTimeOffset < CWACryptography.EKROLLING_PERIOD; rollingTimeOffset++) {
                            try {
                                byte[] infectedRPI = CWACryptography.getRollingProximityIdentifier(infectedTek, (rsin + rollingTimeOffset));
                                if (user.getRpiList().contains(infectedRPI)) {
                                    // user had contact with infected person, so he needs to be alerted

                                }


                            } catch (IllegalBlockSizeException | InvalidKeyException |
                                    BadPaddingException | NoSuchAlgorithmException |
                                    NoSuchPaddingException e) {
                                LOG.log(Level.WARNING, null, e);
                            }
                        }
                    });
                });
    }

    public static ConcurrentHashMap<Integer, List<byte[]>> getCwaData() {
        return cwaData;
    }
}
