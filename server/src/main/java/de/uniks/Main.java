package de.uniks;

import de.uniks.CWA.CWADataParsed;
import de.uniks.CWA.CWARequests;
import de.uniks.SQLite.SQLite;
import de.uniks.spark.SparkRequestHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        SparkRequestHandler.handleRequests();
        scheduler.scheduleAtFixedRate(CWADataParsed::ckeckForInfectionsHourlyTask, 0, 1, TimeUnit.HOURS);

        try {
            Thread springThread = new Thread(SpringBoot::startSpring);
            springThread.start();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "UI Vaadin Spring Boot Thread crashed!", e);
        }
    }

    @Deprecated
    private static void updateCWAKeys() {
        while (true) {
            try {
                SQLite.initializeDatabase();
                SQLite.insertExposures(CWARequests.getUnzippedInfectionData());
                SQLite.cleanUpDatabases();
            } catch (IOException | InterruptedException | SQLException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                //cwaStatus = e.getMessage() + "\n" + sw.toString();
                try {
                    TimeUnit.HOURS.sleep(1);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                continue;
                //return;
            }
            try {
                TimeUnit.HOURS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}