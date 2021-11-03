package de.uniks.spark;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.cwa.utils.CWACryptography;
import de.uniks.postgres.db.PostgresConnect;
import de.uniks.postgres.db.model.InfectedUser;
import de.uniks.postgres.db.model.User;
import de.uniks.postgres.db.utils.InfectedUserPostgreSql;
import de.uniks.postgres.db.utils.UserPostgreSql;
import de.uniks.postgres.db.utils.UserVerificationPostgreSql;
import de.uniks.spark.payload.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static spark.Spark.get;
import static spark.Spark.post;

public class SparkRequestHandler {
    private static final String ROUTING_PREFIX = "/api";
    private static final String SERVICE_SUCCESS = "Success!";
    private static final String SERVICE_INVALID_VERIFY = "Invalid";
    private static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    private static final String SERVICE_REQUEST_BODY_INVALID = "Request body invalid!";
    private static final String SERVICE_REQUEST_VALUES_INVALID = "Request values invalid!";

    private static PostgresConnect dbConnection = new PostgresConnect();
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

        get(ROUTING_PREFIX + "/daylightoffset", (request, response) -> {
            // Creating a TimeZone
            TimeZone offtime_zone = TimeZone.getTimeZone("Europe/Berlin");

            // Creating date object
            Date dt = new Date();

            // Verifying daylight
            Boolean bool_daylight = offtime_zone.inDaylightTime(dt);

            return bool_daylight.toString();
        });

        get(ROUTING_PREFIX + "/uuid", (request, response) -> {
            if (isDBNotConnected()) {
                response.status(HTTP_UNAVAILABLE);
                return SERVICE_UNAVAILABLE;
            }

            String newUuid;
            do {
                newUuid = UUID.randomUUID().toString();
            } while (!userDb.get(newUuid).isEmpty());
            return newUuid;
        });

        get(ROUTING_PREFIX + "/todaysRsin", (request, response) -> {
            Date date = new Date();
            date.setHours(0);
            date.setMinutes(0);
            date.setSeconds(0);
            return CWACryptography.getRollingStartIntervalNumber(date.getTime() / 1000L);
        });

        /**
         * example body:
         * {
         *     "uuid":"d7134243-b21c-4d2f-a9e3-ff0de17608dc",
         *     "status":"0",
         *     "enin":"2697408",
         *     "rpiList":"[[0,24,22,2,111,-57,37,63,-83,33,-55,-77,59,25,-21,29],[0,49,115,-69,90,51,-128,10,28,51,-107,-28,-5,-11,54,-12]]"
         * }
         */
        post(ROUTING_PREFIX + "/data/input", ((request, response) -> {
            if (isDBNotConnected()) {
                response.status(HTTP_UNAVAILABLE);
                return SERVICE_UNAVAILABLE;
            }

            ObjectMapper mapper = new ObjectMapper();
            UserPostPayload input;
            try {
                input = mapper.readValue(request.body(), UserPostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return SERVICE_REQUEST_BODY_INVALID;
            }

            if (!input.isValid()) {
                //TODO: sometimes this if will be entered, even if input is valid?! wtf
                response.status(HTTP_BAD_REQUEST);
                return SERVICE_REQUEST_VALUES_INVALID;
            }

            userDb.save(input.getUserForDB());

            return SERVICE_SUCCESS;
        }));

        post(ROUTING_PREFIX + "/data/input/tek/share", ((request, response) -> {
            if (isDBNotConnected()) {
                response.status(HTTP_UNAVAILABLE);
                return SERVICE_UNAVAILABLE;
            }

            ObjectMapper mapper = new ObjectMapper();
            InfectedUserPostPayload input;
            try {
                input = mapper.readValue(request.body(), InfectedUserPostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return SERVICE_REQUEST_BODY_INVALID;
            }

            // validate TEK and complete infection Data
            if (input.isValid() && infectedUserDb.isIncompleteTekInputEntryPresent(input)) {
                infectedUserDb.completeTekInputEntry(new InfectedUser(input));
                return SERVICE_SUCCESS;
            }
            response.status(HTTP_BAD_REQUEST);
            return SERVICE_REQUEST_BODY_INVALID;
        }));

        post(ROUTING_PREFIX + "/infection/status", (request, response) -> {
            if (isDBNotConnected()) {
                response.status(HTTP_UNAVAILABLE);
                return SERVICE_UNAVAILABLE;
            }

            ObjectMapper mapper = new ObjectMapper();
            UuidPostPayload input;
            try {
                input = mapper.readValue(request.body(), UuidPostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return SERVICE_REQUEST_BODY_INVALID;
            }

            if (!input.isValid()) {
                response.status(HTTP_BAD_REQUEST);
                return SERVICE_REQUEST_BODY_INVALID;
            }

            return isTrackerUserInfected(input.getUuid()) ? "Infected" : "Unknown";
        });

        post(ROUTING_PREFIX + "/verify", (request, response) -> {
            if (isDBNotConnected()) {
                response.status(HTTP_UNAVAILABLE);
                return SERVICE_UNAVAILABLE;
            }

            ObjectMapper mapper = new ObjectMapper();
            UuidPinPostPayload input;
            try {
                input = mapper.readValue(request.body(), UuidPinPostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return SERVICE_REQUEST_BODY_INVALID;
            }

            if (!input.isValid()) {
                response.status(HTTP_BAD_REQUEST);
                return SERVICE_REQUEST_BODY_INVALID;
            }

            Optional<LocalDateTime> localDateTime = verificationDb.completeEntryIfExists(input.getUuid(), input.getPin());

            if (localDateTime.isEmpty()) {
                return SERVICE_INVALID_VERIFY;
            } else {
                return localDateTime.get().toString();
            }
        });

        post(ROUTING_PREFIX + "/verify/update", (request, response) -> {
            if (isDBNotConnected()) {
                response.status(HTTP_UNAVAILABLE);
                return SERVICE_UNAVAILABLE;
            }

            ObjectMapper mapper = new ObjectMapper();
            UuidPinTimePostPayload input;
            try {
                input = mapper.readValue(request.body(), UuidPinTimePostPayload.class);
            } catch (JsonParseException | JsonMappingException e) {
                response.status(HTTP_BAD_REQUEST);
                return SERVICE_REQUEST_BODY_INVALID;
            }

            if (!input.isValid()) {
                response.status(HTTP_BAD_REQUEST);
                return SERVICE_REQUEST_BODY_INVALID;
            }

            // give back the enin for infection date,
            // NOT_INFECTED if no infection is present
            // and WAIT if there is no data from user
            return verificationDb.checkForUserDataInput(input.getUuid(), input.getPin(), input.getTimestamp());
        });
    }

    private static Boolean isDBNotConnected() {
        return dbConnection.getConnection().isEmpty();
    }

    private static Boolean isTrackerUserInfected(String uuid) {
        // first check if there is a Status 2 for infected for that user
        List<User> foundUser = userDb.get(uuid);
        if (!foundUser.isEmpty() && foundUser.stream()
                .filter(user -> user.getStatus().equals(1) || user.getStatus().equals(2)).findAny().isPresent()) {
            return true;
        }

        // second, check if there is a entry in infected table for that user (important for the next 14d)
        Date date = new Date();
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);

        InfectedUserPostPayload infectedUser = new InfectedUserPostPayload();
        infectedUser.setUuid(uuid);
        infectedUser.setEnin(CWACryptography.getRollingStartIntervalNumber(date.getTime() / 1000L));

        if (infectedUserDb.isTekInputEntryPresent(infectedUser)) {
            return true;
        }

        return false;
    }
}
