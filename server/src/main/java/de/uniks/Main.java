package de.uniks;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.CWA.CWARequests;
import de.uniks.SQLite.SQLite;
import de.uniks.payload.InfectionPostPayload;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.net.HttpURLConnection.*;
import static org.sqlite.SQLiteErrorCode.SQLITE_NOTFOUND;
import static spark.Spark.*;

public class Main {
    private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        get("/api/hello", (request, response) -> "Hello World");

        get("/api/cwa/status", (request, response) -> cwaStatus);

        get("/api/infections/rsin", (request, response) -> {
            JSONObject json = new JSONObject();
            Map<Integer, Integer> tablesizes = SQLite.getRSINTableSizes();
            for (Map.Entry<Integer, Integer> entry : tablesizes.entrySet()) {
                json.put(entry.getKey().toString(), entry.getValue());
            }
            return json;
        });

        get("/api/infections/rsin/:rsin", (request, response) -> {
            int rsin;
            try {
                rsin = Integer.parseInt(request.params(":rsin"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number";
            }

            List<byte[]> table;
            try {
                table = SQLite.getRSINTable(rsin);
            } catch (SQLException e) {
                response.status(HTTP_INTERNAL_ERROR);
                return e.getMessage();
            }
            return new JSONArray(table);
        });

        get("/api/infections/rsin/:rsin/teknr/:teknr", (request, response) -> {
            int rsin;
            try {
                rsin = Integer.parseInt(request.params(":rsin"));
                if (Math.ceil(Math.log10(rsin + 1)) != 7) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number and 7 digits long";
            }

            int teknr;
            try {
                teknr = Integer.parseInt(request.params(":teknr"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number";
            }

            byte[] tek;
            Connection connection = null;
            try {
                connection = SQLite.openDatabase();
                tek = SQLite.getKeyData(rsin, teknr, connection);
            } catch (SQLException e) {
                if (e.getErrorCode() == SQLITE_NOTFOUND.code) {
                    response.status(HTTP_NOT_FOUND);
                    return "Key not found";
                } else {
                    response.status(HTTP_INTERNAL_ERROR);
                    return "Error " + e.getErrorCode() + ": " + e.getMessage();
                }
            } finally {
                if (connection != null) {
                    SQLite.closeDatabase(connection);
                }
            }
            return new JSONArray(tek);
        });

        post("/api/infections", ((request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            InfectionPostPayload input;
            try {
                input = mapper.readValue(request.body(), InfectionPostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Request body invalid";
            }

            if (!input.isValid()) {
                response.status(HTTP_BAD_REQUEST);
                return "Request values invalid";
            }

            if (!input.isAuthenticated()) {
                response.status(HTTP_UNAUTHORIZED);
                return "Not authenticated";
            }

            try {
                SQLite.addTemporaryExposureKey(input.getRsin(), input.getKeyData());
            } catch (SQLException e) {
                response.status(HTTP_INTERNAL_ERROR);
                return e.getMessage();
            }
            return "Success!";
        }));

        delete("/api/infections/rsin/:rsin", (request, response) -> {
            int rsin;
            try {
                rsin = Integer.parseInt(request.params(":rsin"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number";
            }

            try {
                SQLite.deleteRollingStartIntervalNumber(rsin);
            } catch (SQLException e) {
                response.status(HTTP_INTERNAL_ERROR);
                return e.getMessage();
            }
            return "Successfully removed " + rsin;
        });

        delete("/api/infections/rsin/:rsin/tek/:tek", (request, response) -> {
            int rsin;
            try {
                rsin = Integer.parseInt(request.params(":rsin"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "RSIN must be number";
            }
            JSONArray json;
            try {
                json = new JSONArray(request.params(":tek"));
            } catch (JSONException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Could not extract array";
            }

            if (json.length() != 16) {
                response.status(HTTP_BAD_REQUEST);
                return "Array must be of length 16";
            }

            byte[] tek = new byte[16];
            for (int i = 0; i < json.length(); i++) {
                tek[i] = (byte) json.getInt(i);
            }

            try {
                SQLite.deleteTemporaryExposureKey(rsin, tek);
            } catch (SQLException e) {
                response.status(HTTP_INTERNAL_ERROR);
                return e.getMessage();
            }

            return "Successfully removed " + Arrays.toString(tek) + " from " + rsin;
        });

        try {
            Thread cwaThread = new Thread(Main::updateCWAKeys);
            cwaThread.start();
        } catch (Exception e) {
            LOGGER.warning("Spark REST API crashed!");
            LOGGER.warning("Exception Message: " + e.getMessage());
        }

        try {
            Thread springThread = new Thread(SpringBoot::startSpring);
            springThread.start();
        } catch (Exception e) {
            LOGGER.warning("UI Vaadin Springboot App crashed!");
            LOGGER.warning("Exception Message: " + e.getMessage());
        }
    }

    private static String cwaStatus = "No update yet";

    private static void updateCWAKeys() {
        while(true) {
            try {
                SQLite.initializeDatabase();
                SQLite.insertExposures(CWARequests.getUnzippedInfectionData());
                SQLite.cleanUpDatabases();
            } catch (IOException | InterruptedException | SQLException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                cwaStatus = e.getMessage() + "\n" + sw.toString();
                try {
                    TimeUnit.HOURS.sleep(1);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                continue;
                //return;
            }

            cwaStatus = "Updated at " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()
                    .truncatedTo(ChronoUnit.SECONDS)).replaceAll("[TZ]", " ") + " UTC";
            LOGGER.info(cwaStatus);

            try {
                TimeUnit.HOURS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}