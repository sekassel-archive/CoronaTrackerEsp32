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
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.*;
import static spark.Spark.*;

public class Main {
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        get("/hello", (request, response) -> "Hello World");

        post("/infections", ((request, response) -> {
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

        get("/infections/rsin", (request, response) -> {
            JSONObject json = new JSONObject();
            Map<Integer, Integer> tablesizes = SQLite.getRSINTableSizes();
            for (Map.Entry<Integer, Integer> entry : tablesizes.entrySet()) {
                json.put(entry.getKey().toString(), entry.getValue());
            }
            return json;
        });

        get("/infections/rsin/:rsin", (request, response) -> {
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

        delete("/infections/rsin/:rsin", (request, response) -> {
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

        delete("/infections/rsin/:rsin/tek/:tek", (request, response) -> {
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

        get("/cwa/status", (request, response) -> cwaStatus);

        executorService.scheduleAtFixedRate(Main::updateCWAKeys, 0, 1, TimeUnit.HOURS);
    }

    private static String cwaStatus;

    private static void updateCWAKeys() {
        try {
            SQLite.initializeDatabase();
            SQLite.insertExposures(CWARequests.getUnzippedInfectionData());
            SQLite.cleanUpDatabases();
        } catch (IOException | InterruptedException | SQLException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            cwaStatus = e.getMessage() + "\n" + sw.toString();
            return;
        }

        cwaStatus = "Updated at " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()
                .truncatedTo(ChronoUnit.SECONDS)).replaceAll("[TZ]", " ") + " UTC";
    }
}