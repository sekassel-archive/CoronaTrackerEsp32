package de.uniks;

import de.uniks.cwa.CwaDataInterpreter;
import de.uniks.spark.SparkRequestHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> future = null;

    public static void main(String[] args) {
        SparkRequestHandler.handleRequests();

        // delay 60 min, scan schedule 3 times a day -> every 8h
        future = scheduler.scheduleAtFixedRate(CwaDataInterpreter::checkForInfectionsHourlyTask, 60, 8 * 60, TimeUnit.MINUTES);
        //TODO: cleanup for login entries in verificationUser DB sometimes

        try {
            Thread springThread = new Thread(SpringBoot::startSpring);
            springThread.start();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "UI Vaadin Spring Boot Thread crashed!", e);
        }
    }

    public static boolean triggerInfectionCheck() {
        if (CwaDataInterpreter.lastCheckTimeString.equals(CwaDataInterpreter.INFECTION_CHECK_IN_PROGRESS)) {
            return false;
        } else {
            future.cancel(true);
            future = scheduler.scheduleAtFixedRate(CwaDataInterpreter::checkForInfectionsHourlyTask, 0, 8 * 60, TimeUnit.MINUTES);
            return true;
        }
    }
}