package de.uniks.postgres.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PostgresConnect {

    private static final Logger LOG = Logger.getLogger(PostgresConnect.class.getName());
    private static Optional<Connection> connection = Optional.empty();

    // DB_URL is not final, because it will be changed in LocalDbConnectionTest through reflections (will not work if field is final)
    //private static String DB_URL = "jdbc:postgresql://database:5432/tracker";
    private static String DB_URL = "jdbc:postgresql://192.168.5.3:5432/tracker";
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
