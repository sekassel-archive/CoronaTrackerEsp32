package de.uniks;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.payload.InfectionPostPayload;
import org.json.JSONObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.net.HttpURLConnection.*;
import static spark.Spark.*;

public class Main {

    private static HashMap<Long, Set<Integer>> infections = new HashMap<>();

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

            //Rounds down to nearest minutes
            long time = Instant.ofEpochSecond(input.getTime()).truncatedTo(ChronoUnit.DAYS).getEpochSecond();
            if (!infections.containsKey(time)) {
                infections.put(time, new HashSet<Integer>() {
                });
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
            int id;
            try {
                id = Integer.parseInt(request.params(":id"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number";
            }

            ArrayList<Long> times = new ArrayList<>();
            for (Map.Entry<Long, Set<Integer>> entry : infections.entrySet()) {
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
            infections = new HashMap<>();
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
            int id;
            try {
                id = Integer.parseInt(request.params(":id"));
            } catch (NumberFormatException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Input must be number";
            }

            ArrayList<Long> times = new ArrayList<>();
            for (Map.Entry<Long, Set<Integer>> entry : infections.entrySet()) {
                if (entry.getValue().contains(id)) {
                    times.add(entry.getKey());
                }
            }

            if (times.isEmpty()) {
                response.status(HTTP_NOT_FOUND);
                return "Input not found";
            }

            for (long key : times) {
                infections.remove(key);
            }

            return "Successfully removed " + id;
        });
    }
}