package de.uniks;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.payload.InfectionPostPayload;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.*;
import static spark.Spark.*;

public class Main {

    private static ConcurrentHashMap<Long, Set<BigInteger>> infections = new ConcurrentHashMap<>();
    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
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
            long time = Instant.ofEpochSecond(input.getTime()).truncatedTo(ChronoUnit.DAYS).getEpochSecond();
            if (!infections.containsKey(time)) {
                infections.put(time, ConcurrentHashMap.newKeySet());
            }
            infections.get(time).add(input.getId());
            return "Success!";
        }));

        //For Debugging purposes
        get("/infections/time/:time", (request, response) -> {
            long time;
            try {
                time = Long.parseLong(request.params(":time"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number";
            }
            time = Instant.ofEpochSecond(time).truncatedTo(ChronoUnit.DAYS).getEpochSecond();

            if (!infections.containsKey(time)) {
                response.status(HTTP_NOT_FOUND);
                return "Input not found";
            }
            return new JSONObject().put("ids", infections.get(time));
        });

        get("/infections/id/:id", (request, response) -> {
            BigInteger id;
            try {
                id = new BigInteger(request.params(":id"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number";
            }

            ArrayList<Long> times = new ArrayList<>();
            for (Map.Entry<Long, Set<BigInteger>> entry : infections.entrySet()) {
                if (entry.getValue().contains(id)) {
                    times.add(entry.getKey());
                }
            }

            if (times.isEmpty()) {
                response.status(HTTP_NOT_FOUND);
                return "Input not found";
            }
            return new JSONObject().put("times", times);
        });

        delete("/infections", ((request, response) -> {
            infections = new ConcurrentHashMap<>();
            return "Successfully removed all entries";
        }));

        delete("/infections/time/:time", (request, response) -> {
            long time;
            try {
                time = Long.parseLong(request.params(":time"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number";
            }
            time = Instant.ofEpochSecond(time).truncatedTo(ChronoUnit.DAYS).getEpochSecond();

            if (!infections.containsKey(time)) {
                response.status(HTTP_NOT_FOUND);
                return "Input not found";
            }
            infections.remove(time);
            return "Successfully removed " + time;
        });

        //delete infections->id
        delete("/infections/id/:id", (request, response) -> {
            BigInteger id;
            try {
                id = new BigInteger(request.params(":id"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number";
            }

            ArrayList<Long> times = new ArrayList<>();
            for (Map.Entry<Long, Set<BigInteger>> entry : infections.entrySet()) {
                if (entry.getValue().contains(id)) {
                    times.add(entry.getKey());
                }
            }

            if (times.isEmpty()) {
                response.status(HTTP_NOT_FOUND);
                return "Input not found";
            }

            for (long key : times) {
                infections.get(key).remove(id);
                if (infections.get(key).isEmpty()) {
                    infections.remove(key);
                }
            }

            return "Successfully removed " + id;
        });

        get("/cwa/status", (request, response) -> cwaStatus);

        executorService.scheduleAtFixedRate(Main::updateCWAKeys, 0, 1, TimeUnit.HOURS);
    }

    private static String cwaStatus;

    private static void updateCWAKeys() {
        Map<Long, List<BigInteger>> keyMap;
        try {
            keyMap = CWARequests.getAllInfectionKeys();
        } catch (IOException | InterruptedException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            cwaStatus = e.getMessage() + "\n" + sw.toString();
            return;
        }
        for (Map.Entry<Long, List<BigInteger>> entry : keyMap.entrySet()) {
            Long time = entry.getKey();

            if (!infections.containsKey(time)) {
                infections.put(time, ConcurrentHashMap.newKeySet());
            }
            Set<BigInteger> idSet = infections.get(time);
            idSet.addAll(entry.getValue());
        }

        cwaStatus = "Updated at " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()
                .truncatedTo(ChronoUnit.SECONDS)).replaceAll("[TZ]", " ") + " UTC";
    }
}