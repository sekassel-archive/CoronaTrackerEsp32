package de.uniks.cwa;

import de.uniks.cwa.utils.CWACryptography;
import de.uniks.cwa.utils.CWARequests;
import de.uniks.postgres.db.model.InfectedUser;
import de.uniks.postgres.db.model.User;
import de.uniks.postgres.db.utils.InfectedUserPostgreSql;
import de.uniks.postgres.db.utils.UserPostgreSql;
import org.springframework.util.StopWatch;

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

    public static final String INFECTION_CHECK_IN_PROGRESS = "Infection check in progress...";
    public static final String NO_INFECTION_CHECK_BY_NOW = "No infection check by now.";

    private static ConcurrentHashMap<Integer, List<byte[]>> cwaData = new ConcurrentHashMap<>();
    private static Collection<InfectedUser> infectedUserList = new ArrayList<>();

    public static String lastCheckTimeString = NO_INFECTION_CHECK_BY_NOW;
    public static StopWatch stopWatch = new StopWatch();

    /**
     * subsequently, we compare contact information from cwa DB with our postgres DB
     * <p>
     * status:
     * - 0: Only entries that needs to be checked. Imply that nothing was found yet.
     * - 1: There was a contact with an infected device. User needs to be warned.
     * - 2: Contact resulted in a proofed infection. User that were in contact needs to be warned.
     * TEK needs to be send from MC to server
     * - 3: A contact with an infected device resulted in a proofed NO infection.
     * Remove Warning, no infection check needed anymore
     */
    public static void checkForInfectionsHourlyTask() {
        try {
            stopWatch.start();

            lastCheckTimeString = INFECTION_CHECK_IN_PROGRESS;
            InfectedUserPostgreSql infectedUserDb = new InfectedUserPostgreSql();
            UserPostgreSql userDb = new UserPostgreSql();

            // cwa db get every 1h a new data set. So we'll get a hourly update of cwaData in memory
            cwaData = CWARequests.getUnzippedInfectionData();

            // our own infectedUser from infectedUser TABLE
            infectedUserList = infectedUserDb.getAllCompleteInfectedUser();

            // we need to get our own infected user into the data set of the cwa and put it in a map like:
            // [enin: rpi(from cwa inf. user tek) .... rpi2 (from our inf. user db)],[enin2: ...] usw.
            ConcurrentHashMap<Integer, List<byte[]>> eninRpisMap = buildInfectedUserEninRpisMap(cwaData, infectedUserList);

            // build sql statements to be queried on our db to check for users with matching rpi from enin rpi Map
            Optional<List<User>> infectedUserActionRequired = buildAndQueryInfectionCheckOnDb(userDb, eninRpisMap);

            if (infectedUserActionRequired.isPresent()) {
                processInfectedUserIntoDb(userDb, infectedUserActionRequired.get());
            }

            lastCheckTimeString = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                    .replaceAll("[TZ]", " ") + " UTC";

            stopWatch.stop();
            LOG.log(Level.INFO, "Hourly cwa data update and check successfully processed at " + lastCheckTimeString);
            LOG.log(Level.INFO, "Total time needed: " + stopWatch.getTotalTimeSeconds() / 60 + " minutes.");

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

    private static void processInfectedUserIntoDb(UserPostgreSql userDb, List<User> infectedUserActionRequired) {
        // status 1: contact with infected device
        // device will be notified and user needs to check himself
        // after that he can use the vaadin UI to input his actual health status to warn other device user
        infectedUserActionRequired.stream().forEach(user -> {
            userDb.updateStatus(user, 1);
        });
    }

    public static ConcurrentHashMap<Integer, List<byte[]>> getCwaData() {
        return cwaData;
    }

    public static Collection<InfectedUser> getInfectedUserList() {
        return infectedUserList;
    }
}
