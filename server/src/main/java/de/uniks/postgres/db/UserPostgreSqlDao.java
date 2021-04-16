package de.uniks.postgres.db;

import de.uniks.postgres.db.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserPostgreSqlDao implements Dao<User, Integer> {

    private static final Logger LOG = Logger.getLogger(UserPostgreSqlDao.class.getName());
    private static Optional<Connection> connection = Optional.empty();

    public UserPostgreSqlDao() {
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
                    LOG.log(Level.SEVERE, null, ex);
                }
            });
        }
    }

    @Override
    public Optional<Integer> save(User user) {
        User nonNullUser = Objects.requireNonNull(user, "The " + User.CLASS + " to be added should not be null");
        String sql = "INSERT INTO " + User.CLASS + "(" + User.UUID + ", " + User.STATUS
                + ", " + User.ENIN + ", " + User.RPILIST + ") " + "VALUES(?, ?, ?, ?)";

        return connection.flatMap(conn -> {
            Optional<Integer> optionalOfInsertedRows = Optional.empty();

            try (PreparedStatement statement = conn.prepareStatement(sql)) {

                statement.setString(1, nonNullUser.getUuid());
                statement.setInt(2, nonNullUser.getStatus());
                statement.setInt(3, nonNullUser.getEnin());
                statement.setString(4, nonNullUser.getRpiListAsJSONArray());

                int numberOfInsertedRows = statement.executeUpdate();
                optionalOfInsertedRows = Optional.of(numberOfInsertedRows);

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            return optionalOfInsertedRows;
        });
    }

    @Override
    public Optional<User> get(String uuid) {
        return connection.flatMap(conn -> {
            Optional<User> user = Optional.empty();
            String sql = "SELECT * FROM " + User.CLASS + " WHERE " + User.UUID + " = \'" + uuid + "\'";
            // TODO: check what to do for more than one column to get with one uuid
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                    if (resultSet.next()) {
                        Integer status = resultSet.getInt(User.STATUS);
                        Integer rsin = resultSet.getInt(User.ENIN);
                        String tekListAsJSONArray = resultSet.getString(User.RPILIST);

                        user = Optional.of(new User(uuid, status, rsin, tekListAsJSONArray));
                    }
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            return user;
        });
    }

    @Override
    public Collection<User> getAll() {
        Collection<User> users = new ArrayList<>();
        String sql = "SELECT * FROM " + User.CLASS;

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
                LOG.log(Level.SEVERE, null, ex);
            }
        });
        return users;
    }

    @Override
    public void update(User user) {
        User nonNullUser = Objects.requireNonNull(user, "The " + User.CLASS + " to be updated should not be null");
        String sql = "UPDATE " + User.CLASS + " "
                + "SET "
                + User.STATUS + " = ?, "
                + User.ENIN + " = ?, "
                + User.RPILIST + " = ? "
                + "WHERE "
                + User.UUID + " = ?";

        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, nonNullUser.getStatus());
                statement.setInt(2, nonNullUser.getEnin());
                statement.setString(3, nonNullUser.getRpiListAsJSONArray());
                statement.setString(4, nonNullUser.getUuid());

                statement.executeUpdate();

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
    }

    @Override
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
}
