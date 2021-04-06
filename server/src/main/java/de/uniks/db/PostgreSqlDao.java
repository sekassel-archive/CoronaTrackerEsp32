package de.uniks.db;

import de.uniks.db.model.Dao;
import de.uniks.db.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PostgreSqlDao implements Dao<User, Integer> {

    private static final Logger LOG = Logger.getLogger(PostgreSqlDao.class.getName());
    private final Optional<Connection> connection;

    public PostgreSqlDao() {
        this.connection = PostgresConnect.getConnection();
    }

    @Override
    public Optional<Integer> save(User user) {
        User nonNullUser = Objects.requireNonNull(user, "The user to be added should not be null");
        String sql = "INSERT INTO " + User.USER_CLASS + "(" + User.USER_UUID + ", " + User.USER_STATUS
                + ", " + User.USER_RSIN + ", " + User.USER_TEKLIST + ") " + "VALUES(?, ?, ?, ?)";

        return connection.flatMap(conn -> {
            Optional<Integer> generatedId = Optional.empty();

            try (PreparedStatement statement =
                         conn.prepareStatement(
                                 sql,
                                 Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, nonNullUser.getUserUuid());
                statement.setInt(2, nonNullUser.getUserStatus());
                statement.setInt(3, nonNullUser.getUserRsin());
                statement.setString(4, nonNullUser.getUserTekListAsJSONArray());

                statement.executeUpdate();

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            return generatedId;
        });
    }

    @Override
    public Optional<User> get(String uuid) {
        return connection.flatMap(conn -> {
            Optional<User> user = Optional.empty();
            String sql = "SELECT * FROM " + User.USER_CLASS + " WHERE " + User.USER_UUID + " = " + uuid;

            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                if (resultSet.next()) {
                    Integer status = resultSet.getInt(User.USER_STATUS);
                    Integer rsin = resultSet.getInt(User.USER_RSIN);
                    String tekListAsJSONArray = resultSet.getString(User.USER_TEKLIST);

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
        String sql = "SELECT * FROM " + User.USER_CLASS;

        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    String uuid = resultSet.getString(User.USER_UUID);
                    Integer status = resultSet.getInt(User.USER_STATUS);
                    Integer rsin = resultSet.getInt(User.USER_RSIN);
                    String tekListAsJSONArray = resultSet.getString(User.USER_TEKLIST);

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
        User nonNullCustomer = Objects.requireNonNull(user, "The customer to be updated should not be null");
        String sql = "UPDATE " + User.USER_CLASS + " "
                + "SET "
                + User.USER_STATUS + " = ?, "
                + User.USER_RSIN + " = ?, "
                + User.USER_TEKLIST + " = ? "
                + "WHERE "
                + User.USER_UUID + " = ?";

        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {

                statement.setInt(1, nonNullCustomer.getUserStatus());
                statement.setInt(2, nonNullCustomer.getUserRsin());
                statement.setString(3, nonNullCustomer.getUserTekListAsJSONArray());
                statement.setString(4, nonNullCustomer.getUserUuid());

                statement.executeUpdate();

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
    }

    @Override
    public void delete(User user) {
        User nonNullCustomer = Objects.requireNonNull(user, "The customer to be deleted should not be null");
        String sql = "DELETE FROM " + User.USER_CLASS + " WHERE " + User.USER_UUID + " = ?";

        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {

                statement.setString(1, nonNullCustomer.getUserUuid());

                statement.executeUpdate();

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
    }
}
