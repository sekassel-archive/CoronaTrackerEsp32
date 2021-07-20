package de.uniks.spark;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.postgres.db.model.User;
import de.uniks.postgres.db.utils.InfectedUserPostgreSql;
import de.uniks.postgres.db.utils.UserPostgreSql;
import de.uniks.postgres.db.utils.UserVerificationPostgreSql;
import de.uniks.spark.payload.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static spark.Spark.get;
import static spark.Spark.post;

public class SparkRequestHandler {
    private static final String ROUTING_PREFIX = "/api";
    private static UserPostgreSql userDb = new UserPostgreSql();
    private static InfectedUserPostgreSql infectedUserDb = new InfectedUserPostgreSql();
    private static UserVerificationPostgreSql verificationDb = new UserVerificationPostgreSql();

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

            // validate TEK and complete infection Data
            if (input.isValid() && infectedUserDb.isIncompleteTekInputEntryPresent()) {
                infectedUserDb.completeTekInputEntry();
                return "Success!";
            }
            response.status(HTTP_BAD_REQUEST);
            return "Request body invalid!";
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

            if (!input.isValid()) {
                response.status(HTTP_BAD_REQUEST);
                return "Request body invalid!";
            }

            return hadContactWithInfectedByStatus(input.getUuid()) ? "Infected" : "Unknown";
        });

        post(ROUTING_PREFIX + "/verify", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            UuidPinPostPayload input;
            try {
                input = mapper.readValue(request.body(), UuidPinPostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Request body invalid!";
            }

            if (!input.isValid()) {
                response.status(HTTP_BAD_REQUEST);
                return "Request body invalid!";
            }

            Optional<LocalDateTime> localDateTime = verificationDb.completeEntryIfExists(input.getUuid(), input.getPin());

            if (localDateTime.isEmpty()) {
                return "Invalid";
            } else {
                return localDateTime.get().toString();
            }
        });

        post(ROUTING_PREFIX + "/verify/update", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            UuidPinTimePostPayload input;
            try {
                input = mapper.readValue(request.body(), UuidPinTimePostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return "Request body invalid!";
            }

            if (!input.isValid()) {
                response.status(HTTP_BAD_REQUEST);
                return "Request body invalid!";
            }

            // give back the enin for infection date,
            // NOT_INFECTED if no infection is present
            // and WAIT if there is no data from user
            return verificationDb.checkForUserDataInput(input.getUuid(), input.getPin(), input.getTimestamp());
        });
    }

    private static Boolean hadContactWithInfectedByStatus(String uuid) {
        List<User> foundUser = userDb.get(uuid);
        if (!foundUser.isEmpty() && foundUser.stream()
                .filter(user -> user.getStatus().equals(1) || user.getStatus().equals(2)).findAny().isPresent()) {
            return true;
        }
        return false;
    }
}
