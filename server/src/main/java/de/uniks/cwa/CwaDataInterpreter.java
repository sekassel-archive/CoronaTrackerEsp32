package de.uniks.cwa;

import de.uniks.cwa.utils.CWACryptography;
import de.uniks.cwa.utils.CWARequests;
import de.uniks.postgres.db.InfectedUserPostgreSqlDao;
import de.uniks.postgres.db.UserPostgreSqlDao;
import de.uniks.postgres.db.model.InfectedUser;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CwaDataInterpreter {

    private static final Logger LOG = Logger.getLogger(CwaDataInterpreter.class.getName());

    /**
     * subsequently, we compare contact information from cwa DB with our postgres DB
     */
    public static void checkForInfectionsHourlyTask() {
        try {
            InfectedUserPostgreSqlDao infectedUserDb = new InfectedUserPostgreSqlDao();
            UserPostgreSqlDao userDb = new UserPostgreSqlDao();

            // cwa db get every 1h a new data set. So we'll get a hourly update of cwaData in memory
            ConcurrentHashMap<Integer, List<byte[]>> cwaData = CWARequests.getUnzippedInfectionData();

            // our own infectedUser from infectedUser TABLE
            Collection<InfectedUser> infectedUserList = infectedUserDb.getAll();

            // we need to get our own infected user into the data set of the cwa and put it in a map like:
            // [enin: rpi(from cwa inf. user tek) .... rpi2 (from our inf. user db)],[enin2: ...] usw.
            ConcurrentHashMap<Integer, List<byte[]>> infectedUser = buildInfectedUserEninRpisMap(cwaData, infectedUserList);

            // build sql statements to be queried on our db to check for users with matching rpi from inf. user map
            Optional<List<User>> infectedUserActionRequired = buildAndQueryInfectionCheckOnDb(userDb, infectedUser);

            if (infectedUserActionRequired.isPresent()) {
                processInfectedUserIntoDb(infectedUserActionRequired.get());
            }

            LOG.log(Level.INFO, "Hourly cwa data update and check successfully processed at " +
                    DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                            .replaceAll("[TZ]", " ") + " UTC");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "CWA Data couldn't process hourly update during getData from CWA Database!", ex);
        }
    }

    private static ConcurrentHashMap<Integer, List<byte[]>> buildInfectedUserEninRpisMap(ConcurrentHashMap<Integer, List<byte[]>> cwaData, Collection<InfectedUser> infectedUserList) {
        ConcurrentHashMap<Integer, List<byte[]>> infectedUserEninRpisMap = new ConcurrentHashMap<>();

        cwaData.forEach((rsin, tekList) -> {
            tekList.forEach(tek -> {
                calculateRpisAndMergeIntoList(infectedUserEninRpisMap, rsin, tek);
            });
        });

        infectedUserList.forEach(infectedUser -> {
            calculateRpisAndMergeIntoList(infectedUserEninRpisMap, infectedUser.getRsin(), infectedUser.getTek());
        });

        return infectedUserEninRpisMap;
    }

    private static void calculateRpisAndMergeIntoList(ConcurrentHashMap<Integer, List<byte[]>> infectedUserEninRpisMap, int rsin, byte[] tek) {
        //TODO: < or <=
        for (int rollingTimeOffset = 0; rollingTimeOffset < CWACryptography.EKROLLING_PERIOD; rollingTimeOffset++) {
            try {
                int enin = (rsin + rollingTimeOffset);
                byte[] infectedRPI = CWACryptography.getRollingProximityIdentifier(tek, enin);
                if (infectedUserEninRpisMap.containsKey(enin)) {
                    infectedUserEninRpisMap.get(enin).add(infectedRPI);
                } else {
                    List<byte[]> rpiList = new ArrayList<>();
                    rpiList.add(infectedRPI);
                    infectedUserEninRpisMap.put(enin, rpiList);
                }
            } catch (IllegalBlockSizeException | InvalidKeyException |
                    BadPaddingException | NoSuchAlgorithmException |
                    NoSuchPaddingException e) {
                LOG.log(Level.WARNING, null, e);
            }
        }
    }

    private static Optional<List<User>> buildAndQueryInfectionCheckOnDb(UserPostgreSqlDao userDb, ConcurrentHashMap<Integer, List<byte[]>> infectedUser) {
        List<User> infUserCollection = new ArrayList<>();
        infectedUser.forEach((enin, rpiList) -> {
            infUserCollection.addAll(userDb.get(enin, rpiList));
        });
        return infUserCollection.isEmpty() ? Optional.empty() : Optional.of(infUserCollection);
    }

    private static void processInfectedUserIntoDb(List<User> infectedUserActionRequired) {
        //TODO: do something with new infected User
    }
}
