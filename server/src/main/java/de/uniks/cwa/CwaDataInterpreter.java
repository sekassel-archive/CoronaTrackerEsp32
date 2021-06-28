package de.uniks.cwa;

import de.uniks.cwa.utils.CWACryptography;
import de.uniks.cwa.utils.CWARequests;
import de.uniks.postgres.db.utils.InfectedUserPostgreSql;
import de.uniks.postgres.db.utils.UserPostgreSql;
import de.uniks.postgres.db.model.InfectedUser;
import de.uniks.postgres.db.model.User;

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

    private static ConcurrentHashMap<Integer, List<byte[]>> cwaData = new ConcurrentHashMap<>();
    private static Collection<InfectedUser> infectedUserList = new ArrayList<>();

    public static String lastCheckTimeString = "No infection check by now.";

    /**
     * subsequently, we compare contact information from cwa DB with our postgres DB
     */
    public static void checkForInfectionsHourlyTask() {
        try {
            lastCheckTimeString = "Infection check in progress...";
            InfectedUserPostgreSql infectedUserDb = new InfectedUserPostgreSql();
            UserPostgreSql userDb = new UserPostgreSql();

            // cwa db get every 1h a new data set. So we'll get a hourly update of cwaData in memory
            cwaData = CWARequests.getUnzippedInfectionData();

            // our own infectedUser from infectedUser TABLE
            infectedUserList = infectedUserDb.getAll();

            // we need to get our own infected user into the data set of the cwa and put it in a map like:
            // [enin: rpi(from cwa inf. user tek) .... rpi2 (from our inf. user db)],[enin2: ...] usw.
            ConcurrentHashMap<Integer, List<byte[]>> eninRpisMap = buildInfectedUserEninRpisMap(cwaData, infectedUserList);

            // build sql statements to be queried on our db to check for users with matching rpi from enin rpi Map
            Optional<List<User>> infectedUserActionRequired = buildAndQueryInfectionCheckOnDb(userDb, eninRpisMap);

            if (infectedUserActionRequired.isPresent()) {
                processInfectedUserIntoDb(infectedUserActionRequired.get());
            }

            lastCheckTimeString = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                    .replaceAll("[TZ]", " ") + " UTC";

            LOG.log(Level.INFO, "Hourly cwa data update and check successfully processed at " + lastCheckTimeString);
        } catch (Exception ex) {
            lastCheckTimeString = "Ups, something went wrong. Waiting for next scheduled infection check.";
            LOG.log(Level.SEVERE, "CWA Data couldn't process hourly update during getData from CWA Database!", ex);
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
        for (int rollingTimeOffset = 0; rollingTimeOffset < CWACryptography.EKROLLING_PERIOD; rollingTimeOffset++) {
            try {
                int enin = (rsin + rollingTimeOffset);
                byte[] infectedRPI = CWACryptography.getRollingProximityIdentifier(tek, enin);
                List<byte[]> rpiListFromMap = infectedUserEninRpisMap.get(enin);
                if (rpiListFromMap != null) {
                    rpiListFromMap.add(infectedRPI);
                } else {
                    List<byte[]> rpiList = new ArrayList<>();
                    rpiList.add(infectedRPI);
                    infectedUserEninRpisMap.put(enin, rpiList);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "calculateRpisAndMergeIntoList exception", e);
            }
        }
    }

    private static Optional<List<User>> buildAndQueryInfectionCheckOnDb(UserPostgreSql userDb, ConcurrentHashMap<Integer, List<byte[]>> infectedUser) {
        List<User> infUserCollection = new ArrayList<>();
        infectedUser.forEach((enin, rpiList) -> {
            try {
                List<User> users = userDb.get(enin, rpiList);
                if (!users.isEmpty()) {
                    infUserCollection.addAll(users);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error happened while query rpiList on userDB, skipped entry", e);
            }
        });
        return infUserCollection.isEmpty() ? Optional.empty() : Optional.of(infUserCollection);
    }

    private static void processInfectedUserIntoDb(List<User> infectedUserActionRequired) {
        //TODO: do something with new infected User
    }

    public static ConcurrentHashMap<Integer, List<byte[]>> getCwaData() {
        return cwaData;
    }

    public static Collection<InfectedUser> getInfectedUserList() {
        return infectedUserList;
    }
}
