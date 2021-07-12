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
import java.util.stream.Collectors;

public class UserVerificationPostgreSql {
    private static final Logger LOG = Logger.getLogger(UserVerificationPostgreSql.class.getName());
    private static Optional<Connection> connection = Optional.empty();

    public UserVerificationPostgreSql() {
        if (connection.isEmpty()) {
            connection = PostgresConnect.getConnection();

            String sql = "CREATE TABLE IF NOT EXISTS public." + VerificationUser.CLASS + " ("
                    + VerificationUser.UUID + " TEXT NOT NULL,"
                    + VerificationUser.PIN + " TEXT,"
                    + VerificationUser.VALID + " BOOLEAN NOT NULL,"
                    + VerificationUser.INFECTED_STATUS + " BOOLEAN,"
                    + VerificationUser.TIMESTAMP + " TEXT NOT NULL)";

            connection.ifPresent(conn -> {
                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, null, ex);
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

        String sql = "INSERT IGNORE INTO " + VerificationUser.CLASS + "(" + VerificationUser.UUID + ", " +
                VerificationUser.VALID + ", " + VerificationUser.TIMESTAMP + ") " + "VALUES(?, ?, ?)";

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
                "AND " + VerificationUser.VALID + " = TRUE";
        List<VerificationUser> users = new ArrayList<>();
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    VerificationUser tmpUser = new VerificationUser();
                    tmpUser.setUuid(resultSet.getString(VerificationUser.UUID));
                    tmpUser.setPin(resultSet.getString(VerificationUser.PIN));
                    tmpUser.setValid(resultSet.getBoolean(VerificationUser.VALID));
                    tmpUser.setTimestamp(LocalDateTime.parse(resultSet.getString(VerificationUser.TIMESTAMP)));
                    tmpUser.setInfectedStatus(resultSet.getBoolean(VerificationUser.INFECTED_STATUS));

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
        if (byUuid.stream().collect(Collectors.groupingBy(VerificationUser::getPin, Collectors.toList())).size() > 1) {
            byUuid.stream().forEach(user -> {
                updateVerificationStatusEntry(user, false);
            });
        }

        // should only be set once
        AtomicReference<Optional<LocalDateTime>> sessionTime = new AtomicReference<>(Optional.empty());

        byUuid.stream().forEach(user -> {
            boolean v = user.getPin().compareTo(pin) == 0;
            updateVerificationStatusEntry(user, v);
            if (sessionTime.get().isEmpty() && v) {
                sessionTime.set(Optional.of(user.getTimestamp()));
            }
        });

        return sessionTime.get();
    }

    private void updateVerificationStatusEntry(VerificationUser user, Boolean verified) {
        String sql = "UPDATE " + VerificationUser.CLASS + " SET " + VerificationUser.INFECTED_STATUS + " = " + verified.toString()
                + " WHERE " + VerificationUser.UUID + " = \'" + user.getUuid() + "\'"
                //+ " AND " + VerificationUser.PIN + " = \'" + user.getPin() + "\'"
                + " AND " + VerificationUser.TIMESTAMP + " = \'" + user.getTimestamp().toString() + "\'";

        connection.flatMap(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {

                int numberOfInsertedRows = statement.executeUpdate();
                LOG.log(Level.INFO, "Updated " + numberOfInsertedRows + " Entry's " + VerificationUser.CLASS + " on DB execute Update.");

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Failed " + VerificationUser.CLASS + " save statement on DB execute Update.", ex);
            }
            return null;
        });
    }

    public Optional<VerificationUser> checkForLoginVerification(String uuid, String timestamp) {
        String sql = "SELECT * FROM " + VerificationUser.CLASS + " WHERE " + VerificationUser.UUID + " = \'" + uuid + "\' " +
                "AND " + VerificationUser.TIMESTAMP + " = \'" + timestamp + "\'";
        AtomicReference<Optional<VerificationUser>> verificationUser = new AtomicReference<>(Optional.empty());
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                if (resultSet.next()) {
                    VerificationUser tmpUser = new VerificationUser();
                    tmpUser.setUuid(resultSet.getString(VerificationUser.UUID));
                    tmpUser.setPin(resultSet.getString(VerificationUser.PIN));
                    tmpUser.setValid(resultSet.getBoolean(VerificationUser.VALID));
                    tmpUser.setTimestamp(LocalDateTime.parse(resultSet.getString(VerificationUser.TIMESTAMP)));
                    tmpUser.setInfectedStatus(resultSet.getBoolean(VerificationUser.INFECTED_STATUS));

                    verificationUser.set(Optional.of(tmpUser));
                }
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "SQL Exception happened in checkForLoginVerification", ex);
            }
        });
        return verificationUser.get();
    }
}
