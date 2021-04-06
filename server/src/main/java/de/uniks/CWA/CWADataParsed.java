package de.uniks.CWA;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CWADataParsed {

    private static final Logger LOG = Logger.getLogger(CWADataParsed.class.getName());
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static ConcurrentHashMap<Integer, List<byte[]>> cwaData = new ConcurrentHashMap<>();

    // TODO: start this in main instead of SQLite hourly update
    public static void ckeckForInfectionsHourlyTask() {
        try {
            // CWA DB get every 1h a new data set. So we'll get a hourly update of cwaData in memory
            cwaData = CWARequests.getUnzippedInfectionData();

            // subsequently, we compare contact information from cwa DB with our postgres DB
            processInfectionCheck();

            LOG.log(Level.INFO, "Hourly CWA data update and check successfully processed at " +
                    DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                            .replaceAll("[TZ]", " ") + " UTC");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "CWA Data couldn't process hourly update during getData from CWA Database!", ex);
        }
    }

    private static void processInfectionCheck() {
        //TODO: start check Infections update
    }

    public static ConcurrentHashMap<Integer, List<byte[]>> getCwaData() {
        return cwaData;
    }
}
