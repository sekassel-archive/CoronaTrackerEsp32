package de.uniks.postgres.db.utils;

import de.uniks.postgres.db.PostgresConnect;
import de.uniks.postgres.db.model.User;
import de.uniks.postgres.db.model.VerificationUser;
import org.eclipse.jetty.util.ajax.JSON;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserPostgreSql {

    private static final Logger LOG = Logger.getLogger(UserPostgreSql.class.getName());
    private static Optional<Connection> connection = Optional.empty();

    public UserPostgreSql() {
        if (connection.isEmpty()) {
            connection = PostgresConnect.getConnection();

            String sql = "CREATE TABLE IF NOT EXISTS public." + User.CLASS + " ("
                    + User.UUID + " TEXT NOT NULL,"
                    + User.STATUS + " INT NOT NULL,"
                    + User.ENIN + " INT NOT NULL,"
                    + User.RPILIST + " TEXT NOT NULL)";

            connection.ifPresent(conn -> {
                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, "Something went wrong while table creation / connection to DB!", ex);
                }
            });
        }
    }

    public Optional<Integer> save(User user) {
        User nonNullUser = Objects.requireNonNull(user, "The " + User.CLASS + " to be added should not be null");
        String sql = "INSERT INTO " + User.CLASS + "(" + User.UUID + ", " + User.STATUS
                + ", " + User.ENIN + ", " + User.RPILIST + ") " + "VALUES(?, ?, ?, ?)";

        AtomicReference<Integer> count = new AtomicReference<>(0);

        nonNullUser.getRpiListAsJSONArray().forEach(tekEntry -> {
            Optional<Integer> integer = connection.flatMap(conn -> {
                Optional<Integer> optionalOfInsertedRows = Optional.empty();
                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    statement.setString(1, nonNullUser.getUuid());
                    statement.setInt(2, nonNullUser.getStatus());
                    statement.setInt(3, nonNullUser.getEnin());
                    statement.setString(4, tekEntry);

                    int numberOfInsertedRows = statement.executeUpdate();
                    optionalOfInsertedRows = Optional.of(numberOfInsertedRows);

                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, "Failed " + User.CLASS + " save statement on DB execute Update.", ex);
                }
                return optionalOfInsertedRows;
            });
            if (integer.isPresent()) {
                count.updateAndGet(v -> v + (int) integer.get());
            }
        });

        LOG.log(Level.INFO, "DB saved user data with " + count.get() + " new entries.");
        return Optional.of(count.get());
    }

    public List<User> get(String uuid) {
        String sql = "SELECT * FROM " + User.CLASS + " WHERE " + User.UUID + " = \'" + uuid + "\'";
        return getWithPrimitiveSql(sql);
    }

    public List<User> getAll() {
        String sql = "SELECT * FROM " + User.CLASS;
        return getWithPrimitiveSql(sql);
    }

    public Integer getUserCount() {
        String sql = "SELECT COUNT(DISTINCT(" + User.UUID + ")) FROM " + User.CLASS;
        AtomicReference<Integer> userCount = new AtomicReference<>(0);
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                if (resultSet.next()) {
                    userCount.set(resultSet.getInt("count"));
                }
            } catch (SQLException ex) {
                LOG.log(Level.WARNING, "SQL Exception happened in getUserCount", ex);
            }
        });
        return userCount.get();
    }

    public List<User> get(Integer enin, List<byte[]> rpiList) throws Exception {
        StringBuilder sql = new StringBuilder();
        // build query to watch out for infected rpis in user db
        // example with shorted rpis: search for infected rpi [1,2,3] in [[1,1,1],[1,2,3],[2,2,2]]
        // SELECT * FROM trackerUser WHERE (enin = 1234567) AND (status = 0) AND
        //  ((rpiList IN ('[[1,2,3]]', '[[2,3,4]]'))
        // [2,3,4] is just an example for more rpis in one query
        sql.append("SELECT * FROM "
                + User.CLASS + " WHERE ("
                + User.ENIN + " = " + enin + ") AND ("
                + User.STATUS + " = 0) AND ("
                + User.RPILIST + " IN (");

        for (byte[] rpiArray : rpiList) {
            sql.append("\'[" + JSON.toString(rpiArray) + "]\',");
        }
        sql.setLength(Math.max(sql.length() - 1, 0));
        sql.append("))");
        return getWithPrimitiveSql(sql.toString());
    }

    private List<User> getWithPrimitiveSql(String sql) {
        List<User> users = new ArrayList<>();
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    String uuid = resultSet.getString(User.UUID);
                    Integer status = resultSet.getInt(User.STATUS);
                    Integer rsin = resultSet.getInt(User.ENIN);
                    String tekListAsJSONArray = resultSet.getString(User.RPILIST);

                    users.add(new User(uuid, status, rsin, tekListAsJSONArray));
                }
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "SQL Exception happened in getWithPrimitiveSql", ex);
            }
        });
        return users;
    }

    // user.getStatus() have to be the old / original one
    public void updateStatus(User user, int newStatus) {
        User nonNullUser = Objects.requireNonNull(user, "The " + User.CLASS + " to be updated should not be null");
        String sql = "UPDATE " + User.CLASS + " "
                + "SET "
                + User.STATUS + " = ? "
                + "WHERE "
                + User.UUID + " = ? AND "
                + User.STATUS + " = ? AND "
                + User.ENIN + " = ? ";
        //+ User.RPILIST + " = ? ";

        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, newStatus);
                statement.setString(2, nonNullUser.getUuid());
                statement.setInt(3, nonNullUser.getStatus());
                statement.setInt(4, nonNullUser.getEnin());

                statement.executeUpdate();

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "updateStatus produced an error on status update", ex);
            }
        });
    }

    public void delete(User user) {
        User nonNullUser = Objects.requireNonNull(user, "The " + User.CLASS + " to be deleted should not be null");
        String sql = "DELETE FROM " + User.CLASS + " WHERE " + User.UUID + " = ?";

        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nonNullUser.getUuid());
                statement.executeUpdate();
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
    }

    public void flagInfectionStateAfterDataInput(String uuid, int rsin, Boolean infectedState) {
        String sqlCleanUpPast = "UPDATE " + User.CLASS + " SET " +
                User.STATUS + " = 3 " + /* 3 = proofed no infection */
                " WHERE " + User.UUID + " = \'" + uuid + "\'" +
                " AND " + User.ENIN + " < \'" + rsin + "\'";

        connection.flatMap(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sqlCleanUpPast)) {
                statement.executeUpdate();
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Failed " + VerificationUser.CLASS + " save statement on DB flagInfectionStateAfterDataInput.", ex);
            }
            return Optional.empty();
        });

        if (infectedState == true) {
            String sqlFlagInfected = "UPDATE " + User.CLASS + " SET " +
                    User.STATUS + " = 2 " + /* 2 = proofed infection */
                    " WHERE " + User.UUID + " = \'" + uuid + "\'" +
                    " AND " + User.ENIN + " >= \'" + rsin + "\'";

            connection.flatMap(conn -> {
                try (PreparedStatement statement = conn.prepareStatement(sqlFlagInfected)) {
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, "Failed " + VerificationUser.CLASS + " save statement on DB flagInfectionStateAfterDataInput.", ex);
                }
                return Optional.empty();
            });
        }

    }
}
