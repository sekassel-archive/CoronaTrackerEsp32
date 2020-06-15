package de.uniks;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.payload.InfectionPostPayload;
import org.json.JSONObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static spark.Spark.get;
import static spark.Spark.post;

public class Main {

    private static HashMap<Long, ArrayList<Integer>> infections = new HashMap<>();

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
            } catch (JsonParseException e) {
                response.status(HTTP_BAD_REQUEST);
                return e.getMessage();
            }

            if(!input.isValid()) {
                response.status(HTTP_BAD_REQUEST);
                return "Request values invalid";
            }

            //Rounds down to nearest minutes
            long time = Instant.ofEpochSecond(input.getTime()).truncatedTo(ChronoUnit.MINUTES).getEpochSecond();
            if(!infections.containsKey(time)) {
                infections.put(time, new ArrayList<>());
            }
            System.out.println(time);
            infections.get(time).add(input.getId());
            return "Success!";
        }));
    }
}