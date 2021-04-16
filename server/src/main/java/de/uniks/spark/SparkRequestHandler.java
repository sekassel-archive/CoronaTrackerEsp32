package de.uniks.spark;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.SQLite.SQLite;
import de.uniks.postgres.db.InfectedUserPostgreSqlDao;
import de.uniks.postgres.db.UserPostgreSqlDao;
import de.uniks.postgres.db.model.User;
import de.uniks.spark.payload.InfectedUserPostPayload;
import de.uniks.spark.payload.InfectionPostPayload;
import de.uniks.spark.payload.UserPostPayload;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static java.net.HttpURLConnection.*;
import static org.sqlite.SQLiteErrorCode.SQLITE_NOTFOUND;
import static spark.Spark.*;
import static spark.Spark.delete;

public class SparkRequestHandler {
    private static final String ROUTING_PREFIX = "/api";
    private static UserPostgreSqlDao userDb = new UserPostgreSqlDao();
    private static InfectedUserPostgreSqlDao infectedUserDb = new InfectedUserPostgreSqlDao();

    public static void handleRequests() {
        // TODO: enable SSL/HTTPS / encrypt traffic
        // http://sparkjava.com/documentation#examples-and-faq
        // https://github.com/tipsy/spark-ssl

        get(ROUTING_PREFIX + "/hello", (request, response) -> "Hello World");

        get(ROUTING_PREFIX + "/uuid", (request, response) -> UUID.randomUUID().toString());

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

            if (!input.isValid()) {
                response.status(HTTP_BAD_REQUEST);
                return "Request values invalid!";
            }

            infectedUserDb.save(input.getInfectedUserForDB());

            return "Success!";
        }));

        get(ROUTING_PREFIX + "/infection/status/:uuid", (request, response) -> {
            String uuid;
            uuid = request.params(":uuid");
            if ((uuid == null) || (uuid.length() != 36)) {
                response.status(HTTP_BAD_REQUEST);
                return "Request values invalid!";
            }

            // TODO: change if get is refactored
            Optional<User> u = userDb.get(uuid);
            if (u.isEmpty()) {
                response.status(HTTP_BAD_REQUEST);
                return "Request couldn't be processed!";
            }

            return new JSONArray(u.get().getStatus());
        });

        /**
         *  below code will be removed
         */
        get(ROUTING_PREFIX + "/infections/rsin/:rsin", (request, response) -> {
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

        get(ROUTING_PREFIX + "/infections/rsin/:rsin/teknr/:teknr", (request, response) -> {
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

        post(ROUTING_PREFIX + "/infections", ((request, response) -> {
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

        delete(ROUTING_PREFIX + "/infections/rsin/:rsin", (request, response) -> {
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

        delete(ROUTING_PREFIX + "/infections/rsin/:rsin/tek/:tek", (request, response) -> {
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

    }
}
