package de.uniks;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.CWA.CWARequests;
import de.uniks.CWA.CWAWebsocket;
import de.uniks.payload.InfectionPostPayload;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.*;
import static spark.Spark.*;

public class Main {

    private static ConcurrentHashMap<Integer, List<byte[]>> infections = new ConcurrentHashMap<>();
    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        webSocket("/cwa", CWAWebsocket.class);

        get("/hello", (request, response) -> "Hello World");

        get("/infections", ((request, response) -> {
            return new JSONObject(infections);
        }));

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

            //Rounds down to nearest day
            int rsin = input.getRsin();
            if (!infections.containsKey(rsin)) {
                infections.put(rsin, new ArrayList<>());
            }
            infections.get(rsin).add(input.getKeyData());
            return "Success!";
        }));

        get("/infections/rsin", (request, response) -> {
            JSONObject json = new JSONObject();
            for (Integer key : infections.keySet()) {
                json.put(key.toString(), infections.get(key).size());
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

            if (!infections.containsKey(rsin)) {
                response.status(HTTP_NOT_FOUND);
                return "Input not found";
            }
            return new JSONArray(infections.get(rsin));
        });

        delete("/infections", ((request, response) -> {
            infections.clear();
            return "Successfully removed all entries";
        }));

        delete("/infections/rsin/:rsin", (request, response) -> {
            int rsin;
            try {
                rsin = Integer.parseInt(request.params(":rsin"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number";
            }

            if (!infections.containsKey(rsin)) {
                response.status(HTTP_NOT_FOUND);
                return "Input not found";
            }
            infections.remove(rsin);
            return "Successfully removed " + rsin;
        });

        delete("/infections/rsin/:rsin/teks/:tek", (request, response) -> {
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
                return "Bad Array";
            }

            if(json.length() != 16) {
                response.status(HTTP_BAD_REQUEST);
                return "Array must be of length 16";
            }

            if (!infections.containsKey(rsin)) {
                response.status(HTTP_NOT_FOUND);
                return "Input not found";
            }

            List<byte[]> bytes = infections.get(rsin);
            byte[] toRemove = null;
            outer:
            for (byte[] key : bytes) {
                for (int i = 0; i < key.length; i++) {
                    if(key[i] != json.getInt(i)) {
                        continue outer;
                    }
                }
                toRemove = key;
                break;
            }

            if(toRemove == null) {
                response.status(HTTP_NOT_FOUND);
                return "Input not found";
            } else {
                bytes.remove(toRemove);
                return "Successfully removed";
            }
        });

        get("/cwa/status", (request, response) -> cwaStatus);

        executorService.scheduleAtFixedRate(Main::updateCWAKeys, 0, 1, TimeUnit.HOURS);
    }

    private static String cwaStatus;

    private static void updateCWAKeys() {
        Map<Integer, List<byte[]>> keyMap;
        try {
            keyMap = CWARequests.getUnzippedInfectionData();
        } catch (IOException | InterruptedException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            cwaStatus = e.getMessage() + "\n" + sw.toString();
            return;
        }

        for (Map.Entry<Integer, List<byte[]>> entry : keyMap.entrySet()) {
            if(!infections.containsKey(entry.getKey())) {
                infections.put(entry.getKey(), new ArrayList<>());
            }
            for (byte[] bytes : entry.getValue()) {
                if(!listContainsArray(infections.get(entry.getKey()),bytes)) {
                    infections.get(entry.getKey()).add(bytes);
                }
            }
        }

        cwaStatus = "Updated at " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()
                .truncatedTo(ChronoUnit.SECONDS)).replaceAll("[TZ]", " ") + " UTC";
    }

    private static boolean listContainsArray(List<byte[]> list, byte[] array) {
        outer:
        for (byte[] bytes : list) {
            if(bytes.length != array.length) {
                continue;
            }

            for (int i = 0; i < bytes.length; i++) {
                if(bytes[i] != array[i]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    public static ConcurrentHashMap<Integer, List<byte[]>> getInfections() {
        return infections;
    }
}