package de.uniks.postgres.db.utils;

import de.uniks.postgres.db.PostgresConnect;
import de.uniks.postgres.db.model.VerificationUser;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserVerificationPostgreSql {
    private static final Logger LOG = Logger.getLogger(UserVerificationPostgreSql.class.getName());
    private static Optional<Connection> connection = Optional.empty();

    public UserVerificationPostgreSql() {
        if (connection.isEmpty()) {
            connection = PostgresConnect.getConnection();

            String sql = "CREATE TABLE IF NOT EXISTS public." + VerificationUser.CLASS + " ("
                    + VerificationUser.UUID + " TEXT NOT NULL,"
                    + VerificationUser.PIN + " TEXT,"
                    + VerificationUser.TOKEN_ACTIVE + " BOOLEAN NOT NULL,"
                    + VerificationUser.TOKEN_VALID + " BOOLEAN DEFAULT FALSE,"
                    + VerificationUser.TIMESTAMP + " TEXT NOT NULL,"
                    + VerificationUser.INPUT_READY_FOR_PICKUP + " BOOLEAN DEFAULT FALSE,"
                    + VerificationUser.USER_INFECTED + " BOOLEAN DEFAULT FALSE,"
                    + VerificationUser.RSIN + " INT DEFAULT NULL"
                    + ")";

            connection.ifPresent(conn -> {
                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, "Something went wrong while table creation / connection to DB!", ex);
                }
            });
        }
    }

    public Optional<Integer> createLoginVerificationEntry(String uuid, String timestampString) {
        List<VerificationUser> presentEntries = getByUuid(uuid);
        if (!presentEntries.isEmpty() && presentEntries.stream()
                .filter(e -> e.getTimestamp().isAfter(LocalDateTime.now().minus(10, ChronoUnit.MINUTES)))
                .count() > 0) {
            return Optional.empty();
        }

        String sql = "INSERT INTO " + VerificationUser.CLASS + "(" + VerificationUser.UUID + ", " +
                VerificationUser.TOKEN_ACTIVE + ", " + VerificationUser.TIMESTAMP + ") " + "VALUES(?, ?, ?)";

        return connection.flatMap(conn -> {
            Optional<Integer> optionalOfInsertedRows = Optional.empty();
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, uuid);
                statement.setBoolean(2, true);
                statement.setString(3, timestampString);

                int numberOfInsertedRows = statement.executeUpdate();
                optionalOfInsertedRows = Optional.of(numberOfInsertedRows);

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Failed " + VerificationUser.CLASS + " save statement on DB execute Update.", ex);
            }
            return optionalOfInsertedRows;
        });
    }

    private List<VerificationUser> getByUuid(String uuid) {
        String sql = "SELECT * FROM " + VerificationUser.CLASS + " WHERE " + VerificationUser.UUID + " = \'" + uuid + "\' " +
                "AND " + VerificationUser.TOKEN_ACTIVE + " = TRUE";
        List<VerificationUser> users = new ArrayList<>();
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    VerificationUser tmpUser = new VerificationUser();
                    tmpUser.setUuid(resultSet.getString(VerificationUser.UUID));
                    tmpUser.setPin(resultSet.getString(VerificationUser.PIN));
                    tmpUser.setTokenActive(resultSet.getBoolean(VerificationUser.TOKEN_ACTIVE));
                    tmpUser.setTimestamp(LocalDateTime.parse(resultSet.getString(VerificationUser.TIMESTAMP)));
                    tmpUser.setTokenValid(resultSet.getBoolean(VerificationUser.TOKEN_VALID));

                    if (tmpUser.getTimestamp().isAfter(LocalDateTime.now().minus(10, ChronoUnit.MINUTES))) {
                        users.add(tmpUser);
                    }
                }
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "SQL Exception happened in getByUuid", ex);
            }
        });
        return users;
    }

    public Optional<LocalDateTime> completeEntryIfExists(String uuid, String pin) {
        // only user from last 10 min with given uuid
        List<VerificationUser> byUuid = getByUuid(uuid);
        if (byUuid.isEmpty()) {
            return Optional.empty();
        }

        // prevent more than one user to be verified to login (if two sessions try to login with same data, abort)
        if (byUuid.stream()
                .filter(user -> user.getTimestamp().isAfter(LocalDateTime.now().minus(10, ChronoUnit.MINUTES)))
                .count() > 1) {
            byUuid.stream().forEach(user -> {
                updateVerificationStatusEntry(user, false);
            });
            return Optional.empty();
        }

        // here should always only exists one valid not verified login entry in db
        VerificationUser verificationUser = byUuid.get(0);
        verificationUser.setPin(pin);
        updateVerificationStatusEntry(verificationUser, true);

        return Optional.of(verificationUser.getTimestamp());
    }

    private void updateVerificationStatusEntry(VerificationUser user, Boolean valid) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE " + VerificationUser.CLASS + " SET " + VerificationUser.TOKEN_VALID + " = " + valid.toString());

        if (valid == true) {
            sql.append(" , " + VerificationUser.PIN + " = \'" + user.getPin() + "\'");
        }

        sql.append(" WHERE " + VerificationUser.UUID + " = \'" + user.getUuid() + "\'" +
                " AND " + VerificationUser.TIMESTAMP + " = \'" + user.getTimestamp().toString() + "\'");

        connection.flatMap(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql.toString())) {

                int numberOfInsertedRows = statement.executeUpdate();
                LOG.log(Level.INFO, "Updated " + numberOfInsertedRows + " Entry's " + VerificationUser.CLASS + " on DB execute Update.");

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Failed " + VerificationUser.CLASS + " save statement on DB execute Update.", ex);
            }
            return Optional.empty();
        });
    }

    public Optional<VerificationUser> checkForLoginVerification(String uuid, String timestamp) {
        String sql = "SELECT * FROM " + VerificationUser.CLASS + " WHERE " + VerificationUser.UUID + " = \'" + uuid + "\' " +
                "AND " + VerificationUser.TIMESTAMP + " = \'" + timestamp + "\'" +
                "AND " + VerificationUser.TOKEN_VALID + " = TRUE";
        AtomicReference<Optional<VerificationUser>> verificationUser = new AtomicReference<>(Optional.empty());
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                if (resultSet.next()) {
                    VerificationUser tmpUser = new VerificationUser();
                    tmpUser.setUuid(resultSet.getString(VerificationUser.UUID));
                    tmpUser.setPin(resultSet.getString(VerificationUser.PIN));
                    tmpUser.setTokenActive(resultSet.getBoolean(VerificationUser.TOKEN_ACTIVE));
                    tmpUser.setTimestamp(LocalDateTime.parse(resultSet.getString(VerificationUser.TIMESTAMP)));
                    tmpUser.setTokenValid(resultSet.getBoolean(VerificationUser.TOKEN_VALID));

                    verificationUser.set(Optional.of(tmpUser));
                }
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "SQL Exception happened in checkForLoginVerification", ex);
            }
        });
        return verificationUser.get();
    }

    public void flagDataInputPickupable(String uuid, String pin, String timestamp, int rsin, boolean infectedState) {
        String sql = "UPDATE " + VerificationUser.CLASS + " SET " +
                VerificationUser.INPUT_READY_FOR_PICKUP + " = TRUE , " +
                VerificationUser.USER_INFECTED + " = " + infectedState + " , " +
                VerificationUser.RSIN + " = " + rsin + " , " +
                " WHERE " + VerificationUser.UUID + " = \'" + uuid + "\'" +
                " AND " + VerificationUser.PIN + " = \'" + pin + "\'" +
                " AND " + VerificationUser.TIMESTAMP + " = \'" + timestamp + "\'";

        connection.flatMap(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql.toString())) {

                int numberOfInsertedRows = statement.executeUpdate();
                LOG.log(Level.INFO, "Updated " + numberOfInsertedRows + " Entry's " + VerificationUser.CLASS + " on DB flagAsInfected.");

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Failed " + VerificationUser.CLASS + " save statement on DB flagAsInfected.", ex);
            }
            return Optional.empty();
        });
    }

    // give back the enin for infection date,
    // NOT_INFECTED if no infection is present
    // and WAIT if there is no data from user
    public String checkForUserDataInput(String uuid, String pin, String timestamp) {
        if (LocalDateTime.parse(timestamp).isBefore(LocalDateTime.now().minus(1, ChronoUnit.DAYS)) ||
                LocalDateTime.parse(timestamp).isAfter(LocalDateTime.now())) {
            return "WAIT";
        }

        String sql = "SELECT * FROM " + VerificationUser.CLASS + " WHERE " +
                VerificationUser.UUID + " = \'" + uuid + "\' AND " +
                VerificationUser.PIN + " = \'" + pin + "\' AND " +
                VerificationUser.TIMESTAMP + " = \'" + timestamp + "\' AND " +
                VerificationUser.INPUT_READY_FOR_PICKUP + " = TRUE";

        AtomicReference<VerificationUser> user = new AtomicReference<>();
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                if (resultSet.next()) {
                    VerificationUser tmpUser = new VerificationUser();
                    tmpUser.setUserInfected(resultSet.getBoolean(VerificationUser.USER_INFECTED));
                    tmpUser.setRsin(resultSet.getInt(VerificationUser.RSIN));
                    user.set(tmpUser);
                }
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "SQL Exception happened in checkForUserDataInput", ex);
            }
        });

        if (user.get() == null) {
            return "WAIT";
        }

        String sqlCleanUp = "UPDATE " + VerificationUser.CLASS + " SET " +
                VerificationUser.INPUT_READY_FOR_PICKUP + " = FALSE , " +
                " WHERE " + VerificationUser.UUID + " = \'" + uuid + "\'" +
                " AND " + VerificationUser.PIN + " = \'" + pin + "\'" +
                " AND " + VerificationUser.TIMESTAMP + " = \'" + timestamp + "\'";

        connection.flatMap(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sqlCleanUp)) {
                int numberOfInsertedRows = statement.executeUpdate();
                LOG.log(Level.INFO, "Updated " + numberOfInsertedRows + " Entry's " + VerificationUser.CLASS + " on DB checkForUserDataInput.");
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Failed " + VerificationUser.CLASS + " save statement on DB checkForUserDataInput.", ex);
            }
            return Optional.empty();
        });

        if (user.get().getUserInfected()) {
            int rsin = user.get().getRsin();
            InfectedUserPostgreSql infUsrDB = new InfectedUserPostgreSql();
            for (int i = 0; i < 14; i++) {
                infUsrDB.createIncompleteTekInputEntry(uuid, rsin + (144 * i));
            }
            return String.valueOf(user.get().getRsin());
        } else {
            return "NOT_INFECTED";
        }
    }
}
