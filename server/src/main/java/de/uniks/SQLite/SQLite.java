package de.uniks.SQLite;

import de.uniks.CWA.CWACryptography;
import org.sqlite.SQLiteException;
import org.sqlite.core.Codes;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import static de.uniks.CWA.CWARequests.getUnzippedInfectionData;
import static org.sqlite.SQLiteErrorCode.SQLITE_NOTFOUND;

public class SQLite {

    private static final String DATABASE_PATH = "jdbc:sqlite:ExposureDatabase.db";

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        try {
            initializeDatabase();
            insertExposures(getUnzippedInfectionData());
        } catch (SQLException | InterruptedException | IOException throwables) {
            throwables.printStackTrace();
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start + " Milliseconds");
    }

    public static void initializeDatabase() throws SQLException {
        final String CREATE_RSIN_SQL = "CREATE TABLE IF NOT EXISTS RSIN (rsin integer UNIQUE);";

        try (Connection conn = DriverManager.getConnection(DATABASE_PATH); Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_RSIN_SQL);
        }
    }

    public static void insertExposures(Map<Integer, List<byte[]>> exposures) throws SQLException {
        final String INSERT_SQL = "INSERT INTO RSIN_?? VALUES(?);";

        try (Connection conn = DriverManager.getConnection(DATABASE_PATH)) {
            createRSINTables(exposures.keySet(), conn);

            for (Map.Entry<Integer, List<byte[]>> entry : exposures.entrySet()) {
                String sql = INSERT_SQL.replace("??", entry.getKey().toString());
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    for (byte[] key_data : entry.getValue()) {
                        pstmt.setBytes(1, key_data);
                        try {
                            pstmt.executeUpdate();
                        } catch (SQLiteException e) {
                            if (e.getErrorCode() != Codes.SQLITE_CONSTRAINT) {
                                throw e; //SQLITE_CONSTRAINT simply means that the value already exists
                            }
                        }
                    }
                }
            }
        }
    }

    private static void createRSINTables(Set<Integer> rsins, Connection db) throws SQLException {
        final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS RSIN_?? (key_data BLOB UNIQUE);"; //TODO Could use AUTOINCREMENT to support deletion of entries in the future
        final String INSERT_SQL = "INSERT INTO RSIN VALUES (?)";

        try (Statement stmt = db.createStatement(); PreparedStatement pstmt = db.prepareStatement(INSERT_SQL)) {
            for (Integer rsin : rsins) {
                stmt.addBatch(CREATE_SQL.replace("??", rsin.toString()));
                pstmt.setInt(1, rsin);
                try {
                    pstmt.executeUpdate();
                } catch (SQLiteException e) {
                    if (e.getErrorCode() != Codes.SQLITE_CONSTRAINT) {
                        throw e; //SQLITE_CONSTRAINT simply means that the value already exists
                    }
                }
            }
            stmt.executeBatch();
        }
    }

    public static Map<Integer, Integer> getRSINTableSizes() throws SQLException {
        final String SELECT_SQL = "SELECT rsin FROM RSIN";
        Map<Integer, Integer> sizesMap = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(DATABASE_PATH);
             Statement stmt = conn.createStatement();
             ResultSet resultSet = stmt.executeQuery(SELECT_SQL)) {
            while (resultSet.next()) {
                int rsin = resultSet.getInt("rsin");
                int size = getTableSize(Integer.toString(rsin), conn);
                sizesMap.put(rsin, size);
            }
            return sizesMap;
        }
    }

    //Deletes databases older than 4 weeks
    public static void cleanUpDatabases() throws SQLException {
        final String SELECT_SQL = "SELECT rsin FROM RSIN WHERE rsin < ?";
        int cutoff = CWACryptography.getRollingStartIntervalNumber(System.currentTimeMillis() / 1000)
                - (CWACryptography.TWO_WEEKS_IN_10_MINUTES_INTERVAL * 2 + 144); //CWA keeps rsins for a month it seems


        try (Connection conn = DriverManager.getConnection(DATABASE_PATH)) {
            ArrayList<Integer> rsinList = new ArrayList<>();

            try (PreparedStatement pstmt = conn.prepareStatement(SELECT_SQL)) {
                pstmt.setInt(1, cutoff);
                try (ResultSet resultSet = pstmt.executeQuery()) {
                    while (resultSet.next()) {
                        rsinList.add(resultSet.getInt("rsin"));
                    }
                }
            }

            final String DELETE_SQL = "DELETE FROM RSIN WHERE rsin < ?";

            try (PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {
                pstmt.setInt(1, cutoff);
                pstmt.execute();
            }
            try (Statement statement = conn.createStatement()) {
                for (Integer rsin : rsinList) {
                    final String DROP_SQL = "DROP TABLE RSIN_" + rsin;
                    statement.addBatch(DROP_SQL);
                }
                statement.executeBatch();
            }
        }
    }

    private static int getTableSize(String name, Connection db) throws SQLException {
        final String SELECT_SQL = "SELECT COUNT(*) FROM RSIN_" + name;

        try (Statement stmt = db.createStatement(); ResultSet resultSet = stmt.executeQuery(SELECT_SQL)) {
            return resultSet.getInt(1);
        }
    }

    //For Debugging purposes

    public static void addTemporaryExposureKey(int rsin, byte[] tek) throws SQLException {
        final String SELECT_SQL = "INSERT INTO RSIN_?? VALUES (?)".replace("??", Integer.toString(rsin));

        try (Connection conn = DriverManager.getConnection(DATABASE_PATH)) {
            HashSet<Integer> rsinSet = new HashSet<>();
            rsinSet.add(rsin);
            createRSINTables(rsinSet, conn);
            try (PreparedStatement pstmt = conn.prepareStatement(SELECT_SQL)) {
                pstmt.setBytes(1, tek);
                pstmt.execute();
            }
        }
    }

    public static List<byte[]> getRSINTable(int rsin) throws SQLException {
        final String SELECT_SQL = "SELECT * FROM RSIN_??".replace("??", Integer.toString(rsin));

        try (Connection conn = DriverManager.getConnection(DATABASE_PATH);
             Statement stmt = conn.createStatement();
             ResultSet resultSet = stmt.executeQuery(SELECT_SQL)) {
            ArrayList<byte[]> table = new ArrayList<>();
            while (resultSet.next()) {
                table.add(resultSet.getBytes(1));
            }

            return table;
        }
    }

    public static void deleteRollingStartIntervalNumber(int rsin) throws SQLException {
        final String DROP_SQL = "DROP TABLE RSIN_??".replace("??", Integer.toString(rsin));

        try (Connection conn = DriverManager.getConnection(DATABASE_PATH); Statement stmt = conn.createStatement()) {
            stmt.execute(DROP_SQL);
        }
    }

    public static void deleteTemporaryExposureKey(int rsin, byte[] tek) throws SQLException {
        final String DELETE_SQL = "DELETE FROM RSIN_?? WHERE key_data = ?".replace("??", Integer.toString(rsin));

        try (Connection conn = DriverManager.getConnection(DATABASE_PATH); PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {
            stmt.setBytes(1, tek);
            stmt.execute(); //TODO Could use update count to confirm if entry even existed before
        }
    }

    //For Websocket

    public static Connection openDatabase() throws SQLException {
        return DriverManager.getConnection(DATABASE_PATH);
    }

    public static byte[] getKeyData(int rsin, int index, Connection db) throws SQLException {
        final String SELECT_SQL = "SELECT key_data FROM RSIN_" + rsin + " WHERE rowid=" + (index + 1);//Database is not zero indexed

        try (Statement stmt = db.createStatement(); ResultSet resultSet = stmt.executeQuery(SELECT_SQL)) {
            if (resultSet.next()) {
                return resultSet.getBytes(1);
            } else {
                throw new SQLiteException("Not Found", SQLITE_NOTFOUND);
            }
        }
    }

    public static void closeDatabase(Connection db) throws SQLException {
        db.close();
    }
}
