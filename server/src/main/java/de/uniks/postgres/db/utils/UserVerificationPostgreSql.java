package de.uniks.postgres.db.utils;

import de.uniks.postgres.db.PostgresConnect;
import de.uniks.postgres.db.model.VerificationUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
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
                    + VerificationUser.STATUS + " BOOLEAN NOT NULL,"
                    + VerificationUser.PIN + " TEXT NOT NULL,"
                    + VerificationUser.TIMESTAMP + " DATE NOT NULL)";

            connection.ifPresent(conn -> {
                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            });
        }
    }


}
