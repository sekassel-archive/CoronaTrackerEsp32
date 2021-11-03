package de.uniks.postgres.db.utils;

import de.uniks.postgres.db.PostgresConnect;
import de.uniks.postgres.db.model.InfectedUser;
import de.uniks.spark.payload.InfectedUserPostPayload;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InfectedUserPostgreSql {

    private static final Logger LOG = Logger.getLogger(InfectedUserPostgreSql.class.getName());
    private static Optional<Connection> connection = Optional.empty();

    public InfectedUserPostgreSql() {
        if (connection.isEmpty()) {
            connection = PostgresConnect.getConnection();

            String sql = "CREATE TABLE IF NOT EXISTS public." + InfectedUser.CLASS + " ("
                    + InfectedUser.UUID + " TEXT NOT NULL,"
                    + InfectedUser.TEK + " TEXT DEFAULT NULL,"
                    + InfectedUser.RSIN + " INT NOT NULL)";

            connection.ifPresent(conn -> {
                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, "Something went wrong while table creation / connection to DB!", ex);
                }
            });
        }
    }

    public List<InfectedUser> get(String uuid) {
        List<InfectedUser> infUsers = new ArrayList<>();
        String sql = "SELECT * FROM " + InfectedUser.CLASS + " WHERE " + InfectedUser.UUID + " = \'" + uuid + "\'";
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    String tek = resultSet.getString(InfectedUser.TEK);
                    Integer rsin = resultSet.getInt(InfectedUser.RSIN);

                    infUsers.add(new InfectedUser(uuid, tek, rsin));
                }
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
        return infUsers;
    }

    public List<InfectedUser> getAllCompleteInfectedUser() {
        List<InfectedUser> infUserCollection = new ArrayList<>();
        String sql = "SELECT * FROM " + InfectedUser.CLASS +
                " WHERE " + InfectedUser.TEK + " IS NOT NULL";

        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    String uuid = resultSet.getString(InfectedUser.UUID);
                    String tek = resultSet.getString(InfectedUser.TEK);
                    Integer rsin = resultSet.getInt(InfectedUser.RSIN);

                    infUserCollection.add(new InfectedUser(uuid, tek, rsin));
                }
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Couldn't get all completed Entries of infected Users from DB.", ex);
            }
        });
        return infUserCollection;
    }

    public Optional<Integer> createIncompleteTekInputEntry(String uuid, int rsin) {
        String sql = "INSERT INTO " + InfectedUser.CLASS + "(" + InfectedUser.UUID + ", " + InfectedUser.RSIN + ") " + "VALUES(?, ?)";

        return connection.flatMap(conn -> {
            Optional<Integer> optionalOfInsertedRows = Optional.empty();

            try (PreparedStatement statement = conn.prepareStatement(sql)) {

                statement.setString(1, uuid);
                statement.setInt(2, rsin);

                int numberOfInsertedRows = statement.executeUpdate();
                optionalOfInsertedRows = Optional.of(numberOfInsertedRows);

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Failed to create initial TEK Entry in DB for uuid:" + uuid + " and rsin: " + rsin, ex);
            }
            return optionalOfInsertedRows;
        });
    }

    public boolean isIncompleteTekInputEntryPresent(InfectedUserPostPayload input) {
        String sql = "SELECT COUNT(*)" +
                " FROM " + InfectedUser.CLASS +
                " WHERE " + InfectedUser.UUID + " = \'" + input.getUuid() + "\'" +
                " AND " + InfectedUser.TEK + " IS NULL" +
                " AND " + InfectedUser.RSIN + " = \'" + input.getRsin() + "\'";
        AtomicReference<Integer> userCount = new AtomicReference<>(0);
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                if (resultSet.next()) {
                    userCount.set(resultSet.getInt("count"));
                }
            } catch (SQLException ex) {
                LOG.log(Level.WARNING, "SQL Exception happened in isIncompleteTekInputEntryPresent", ex);
            }
        });
        return userCount.get() >= 1 ? true : false;
    }

    public boolean isTekInputEntryPresent(InfectedUserPostPayload input) {
        String sql = "SELECT COUNT(*)" +
                " FROM " + InfectedUser.CLASS +
                " WHERE " + InfectedUser.UUID + " = \'" + input.getUuid() + "\'" +
                " AND " + InfectedUser.RSIN + " = \'" + input.getRsin() + "\'";
        AtomicReference<Integer> userCount = new AtomicReference<>(0);
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                if (resultSet.next()) {
                    userCount.set(resultSet.getInt("count"));
                }
            } catch (SQLException ex) {
                LOG.log(Level.WARNING, "SQL Exception happened in isTekInputEntryPresent", ex);
            }
        });
        return userCount.get() >= 1 ? true : false;
    }

    public void completeTekInputEntry(InfectedUser infectedUser) {
        InfectedUser nonNullUser = Objects.requireNonNull(infectedUser, "The " + InfectedUser.CLASS + " to be updated should not be null");
        String sql = "UPDATE " + InfectedUser.CLASS + " "
                + "SET "
                + InfectedUser.TEK + " = ? "
                + "WHERE "
                + InfectedUser.RSIN + " = ? "
                + "AND "
                + InfectedUser.UUID + " = ?";

        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nonNullUser.getTekAsJSONArray());
                statement.setInt(2, nonNullUser.getRsin());
                statement.setString(3, nonNullUser.getUuid());

                statement.executeUpdate();

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Couldn't complete TEK Entry for infected User input in DB.", ex);
            }
        });
    }

    public void delete(String uuid) {
        String sql = "DELETE FROM " + InfectedUser.CLASS + " WHERE " + InfectedUser.UUID + " = ?";

        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, uuid);
                statement.executeUpdate();

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
    }

    public void cleanup() {
        String sql = "DELETE FROM " + InfectedUser.CLASS + " WHERE " + InfectedUser.RSIN + " < ?";
        int eninNow = Math.toIntExact(Instant.now().getEpochSecond() / 600L);


        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1,  (eninNow - (144 * 14)));
                statement.executeUpdate();

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Error while removing old InfectedUserData", ex);
            }
        });
    }
}
