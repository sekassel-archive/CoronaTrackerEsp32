package de.uniks.postgres.db.utils;

import de.uniks.postgres.db.PostgresConnect;
import de.uniks.postgres.db.model.InfectedUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
                    + InfectedUser.TEK + " TEXT NOT NULL,"
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

    public Optional<Integer> save(InfectedUser infUser) {
        InfectedUser nonNullUser = Objects.requireNonNull(infUser, "The " + InfectedUser.CLASS + " to be added should not be null");
        String sql = "INSERT INTO " + InfectedUser.CLASS + "(" + InfectedUser.UUID + ", " + InfectedUser.TEK + ", "
                + InfectedUser.RSIN + ") " + "VALUES(?, ?, ?)";

        return connection.flatMap(conn -> {
            Optional<Integer> optionalOfInsertedRows = Optional.empty();

            try (PreparedStatement statement = conn.prepareStatement(sql)) {

                statement.setString(1, nonNullUser.getUuid());
                statement.setString(2, nonNullUser.getTekAsJSONArray());
                statement.setInt(3, nonNullUser.getRsin());

                int numberOfInsertedRows = statement.executeUpdate();
                optionalOfInsertedRows = Optional.of(numberOfInsertedRows);

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            return optionalOfInsertedRows;
        });
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

    public List<InfectedUser> getAll() {
        List<InfectedUser> infUserCollection = new ArrayList<>();
        String sql = "SELECT * FROM " + InfectedUser.CLASS;

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
                LOG.log(Level.SEVERE, null, ex);
            }
        });
        return infUserCollection;
    }

    public void update(InfectedUser infUser) {
        InfectedUser nonNullUser = Objects.requireNonNull(infUser, "The " + InfectedUser.CLASS + " to be updated should not be null");
        String sql = "UPDATE " + InfectedUser.CLASS + " "
                + "SET "
                + InfectedUser.TEK + " = ?, "
                + InfectedUser.RSIN + " = ? "
                + "WHERE "
                + InfectedUser.UUID + " = ?";

        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nonNullUser.getTekAsJSONArray());
                statement.setInt(2, nonNullUser.getRsin());
                statement.setString(3, nonNullUser.getUuid());

                statement.executeUpdate();

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
    }

    public void delete(InfectedUser infUser) {
        InfectedUser nonNullInfUser = Objects.requireNonNull(infUser, "The " + InfectedUser.CLASS + " to be deleted should not be null");
        String sql = "DELETE FROM " + InfectedUser.CLASS + " WHERE " + InfectedUser.UUID + " = ?";

        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nonNullInfUser.getUuid());
                statement.executeUpdate();

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
    }
}
