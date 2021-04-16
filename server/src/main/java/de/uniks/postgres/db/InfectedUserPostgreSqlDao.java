package de.uniks.postgres.db;

import de.uniks.postgres.db.model.InfectedUser;
import de.uniks.postgres.db.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InfectedUserPostgreSqlDao implements Dao<InfectedUser, Integer> {

    private static final Logger LOG = Logger.getLogger(InfectedUserPostgreSqlDao.class.getName());
    private static Optional<Connection> connection = Optional.empty();

    public InfectedUserPostgreSqlDao() {
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
                    LOG.log(Level.SEVERE, null, ex);
                }
            });
        }
    }


    @Override
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

    @Override
    public Optional<InfectedUser> get(String uuid) {
        return connection.flatMap(conn -> {
            Optional<InfectedUser> infUser = Optional.empty();
            String sql = "SELECT * FROM " + InfectedUser.CLASS + " WHERE " + InfectedUser.UUID + " = \'" + uuid + "\'";

            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                if (resultSet.next()) {
                    String tek = resultSet.getString(InfectedUser.TEK);
                    Integer rsin = resultSet.getInt(InfectedUser.RSIN);

                    infUser = Optional.of(new InfectedUser(uuid, tek, rsin));
                }
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            return infUser;
        });
    }

    @Override
    public Collection<InfectedUser> getAll() {
        Collection<InfectedUser> infUserCollection = new ArrayList<>();
        String sql = "SELECT * FROM " + User.CLASS;

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

    @Override
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

    @Override
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
