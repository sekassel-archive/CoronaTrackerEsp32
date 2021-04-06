package de.uniks.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PostgresConnect {

    private static final Logger LOG = Logger.getLogger(PostgresConnect.class.getName());
    private static Optional<Connection> connection = Optional.empty();

    private static final String DB_URL = "jdbc:postgresql://database:5432/tracker";
    private static final String USER = "tracker";
    private static final String PASS = "mk2G4FsO8wah1tp4TqqT";

    public static Optional<Connection> getConnection() {
        if (connection.isEmpty()) {
            try {
                connection = Optional.ofNullable(DriverManager.getConnection(DB_URL, USER, PASS));
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, "Can't connect to postgres DB!", ex);
            }
        }
        return connection;
    }
}
