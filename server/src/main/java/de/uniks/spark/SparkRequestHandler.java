package de.uniks.spark;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.postgres.db.InfectedUserPostgreSqlDao;
import de.uniks.postgres.db.UserPostgreSqlDao;
import de.uniks.postgres.db.model.User;
import de.uniks.spark.payload.InfectedUserPostPayload;
import de.uniks.spark.payload.UserPostPayload;
import de.uniks.spark.payload.UuidPostPayload;

import java.util.List;
import java.util.UUID;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static spark.Spark.get;
import static spark.Spark.post;

public class SparkRequestHandler {
    private static final String ROUTING_PREFIX = "/api";
    private static UserPostgreSqlDao userDb = new UserPostgreSqlDao();
    private static InfectedUserPostgreSqlDao infectedUserDb = new InfectedUserPostgreSqlDao();

    public static void handleRequests() {
        // TODO: enable SSL/HTTPS / encrypt traffic
        // http://sparkjava.com/documentation#examples-and-faq
        // https://github.com/tipsy/spark-ssl

        get(ROUTING_PREFIX + "/ping", (request, response) -> {
            return "pong";
        });

        get(ROUTING_PREFIX + "/uuid", (request, response) -> {
            String newUuid;
            do {
                newUuid = UUID.randomUUID().toString();
            } while (!userDb.get(newUuid).isEmpty());
            return newUuid;
        });

        /**
         * example body:
         * {
         *     "uuid":"d7134243-b21c-4d2f-a9e3-ff0de17608dc",
         *     "status":"2",
         *     "enin":"2697408",
         *     "rpiList":"[[0,24,22,2,111,-57,37,63,-83,33,-55,-77,59,25,-21,29],[0,49,115,-69,90,51,-128,10,28,51,-107,-28,-5,-11,54,-12]]"
         * }
         */
        post(ROUTING_PREFIX + "/data/input", ((request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            UserPostPayload input;
            try {
                input = mapper.readValue(request.body(), UserPostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Request body invalid!";
            }

            if (!input.isValid()) {
                response.status(HTTP_BAD_REQUEST);
                return "Request values invalid!";
            }

            userDb.save(input.getUserForDB());

            return "Success!";
        }));

        post(ROUTING_PREFIX + "/data/input/tek/share", ((request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            InfectedUserPostPayload input;
            try {
                input = mapper.readValue(request.body(), InfectedUserPostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Request body invalid!";
            }

            if (input.isValid() && hasContactWithInfectedByStatus(input.getUuid())) {
                infectedUserDb.save(input.getInfectedUserForDB());
                return "Success!";
            }
            response.status(HTTP_BAD_REQUEST);
            return "Request values invalid!";
        }));

        post(ROUTING_PREFIX + "/infection/status", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            UuidPostPayload input;
            try {
                input = mapper.readValue(request.body(), UuidPostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Request body invalid!";
            }

            if(!input.isValid()){
                response.status(HTTP_BAD_REQUEST);
                return "Request body invalid!";
            }

            return hasContactWithInfectedByStatus(input.getUuid()) ? "Infected" : "Unknown";
        });
    }

    private static Boolean hasContactWithInfectedByStatus(String uuid) {
        List<User> foundUser = userDb.get(uuid);
        if (!foundUser.isEmpty() && foundUser.stream().filter(user -> !user.getStatus().equals(0)).findAny().isPresent()) {
            return true;
        }
        return false;
    }
}
