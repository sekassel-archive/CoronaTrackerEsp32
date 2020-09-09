package de.uniks.SQLite;

import de.uniks.CWA.CWACryptography;
import org.sqlite.SQLiteException;
import org.sqlite.core.Codes;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import static de.uniks.CWA.CWARequests.getUnzippedInfectionData;

public class SQLite {

    private static final String DATABASE_PATH = "jdbc:sqlite:ExposureDatabase.db";

    private static final String CREATE_RSIN_SQL = "CREATE TABLE IF NOT EXISTS RSIN (rsin integer UNIQUE);";

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
        Connection conn = DriverManager.getConnection(DATABASE_PATH);

        Statement stmt = conn.createStatement();
        stmt.execute(CREATE_RSIN_SQL);

        conn.close();
    }

    public static void insertExposures(Map<Integer, List<byte[]>> exposures) throws SQLException {
        final String INSERT_SQL = "INSERT INTO RSIN_?? VALUES(?);";

        Connection conn = DriverManager.getConnection(DATABASE_PATH);
        createRSINTables(exposures.keySet(), conn);

        for (Map.Entry<Integer, List<byte[]>> entry : exposures.entrySet()) {
            String sql = INSERT_SQL.replace("??", entry.getKey().toString());
            PreparedStatement pstmt = conn.prepareStatement(sql);
            for (byte[] key_data : entry.getValue()) {
                pstmt.setBytes(1, key_data);
                try {
                    pstmt.executeUpdate();
                } catch (SQLiteException e) {
                    if (e.getErrorCode() != Codes.SQLITE_CONSTRAINT) throw e;
                }
            }
        }

        conn.close();
    }

    private static void createRSINTables(Set<Integer> rsins, Connection db) throws SQLException {
        final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS RSIN_? (key_data BLOB UNIQUE);";
        final String INSERT_SQL = "INSERT INTO RSIN VALUES (?)";

        Statement stmt = db.createStatement();
        PreparedStatement pstmt = db.prepareStatement(INSERT_SQL);

        for (Integer rsin : rsins) {
            String sql = CREATE_SQL.replace("?", rsin.toString());
            stmt.execute(sql);
            pstmt.setInt(1, rsin);
            try {
                pstmt.executeUpdate();
            } catch (SQLiteException e) {
                if (e.getErrorCode() != Codes.SQLITE_CONSTRAINT)
                    throw e; //SQLITE_CONSTRAINT simply means that the value already exists
            }
        }
    }

    public static Map<Integer, Integer> getRSINTableSizes() throws SQLException {
        final String SELECT_SQL = "SELECT rsin FROM RSIN";
        Map<Integer, Integer> sizesMap = new HashMap<>();

        Connection conn = DriverManager.getConnection(DATABASE_PATH);
        ResultSet resultSet = conn.createStatement().executeQuery(SELECT_SQL);

        while (resultSet.next()) {
            int rsin = resultSet.getInt("rsin");
            int size = getTableSize(Integer.toString(rsin), conn);
            sizesMap.put(rsin, size);
        }
        return sizesMap;
    }

    //Deletes databases older than 2 weeks
    public static void cleanUpDatabases() throws SQLException {
        final String SELECT_SQL = "SELECT rsin FROM RSIN WHERE rsin < ?";
        int cutoff = CWACryptography.getRollingStartIntervalNumber(System.currentTimeMillis() / 1000)
                - (CWACryptography.TWO_WEEKS_IN_10_MINUTES_INTERVAL * 2 + 144); //CWA keeps rsins for a month it seems

        Connection conn = DriverManager.getConnection(DATABASE_PATH);

        PreparedStatement stmt = conn.prepareStatement(SELECT_SQL);
        stmt.setInt(1, cutoff);
        ResultSet resultSet = stmt.executeQuery();

        ArrayList<Integer> rsinList = new ArrayList<>();
        while (resultSet.next()) {
            rsinList.add(resultSet.getInt("rsin"));
        }
        final String DELETE_SQL = "DELETE FROM RSIN WHERE rsin < ?";
        stmt = conn.prepareStatement(DELETE_SQL);
        stmt.setInt(1, cutoff);
        stmt.execute();

        Statement statement = conn.createStatement();
        for (Integer rsin : rsinList) {
            final String DROP_SQL = "DROP TABLE RSIN_" + rsin;
            statement.addBatch(DROP_SQL);
        }
        statement.executeBatch();
    }

    private static int getTableSize(String name, Connection db) throws SQLException {
        final String SELECT_SQL = "SELECT COUNT(*) FROM RSIN_" + name;
        return db.createStatement().executeQuery(SELECT_SQL).getInt(1);
    }

    public static Connection openDatabase() throws SQLException {
        return DriverManager.getConnection(DATABASE_PATH);
    }

    public static void closeDatabase(Connection db) throws SQLException {
        db.close();
    }

    public static byte[] getKeyData(int rsin, int index, Connection db) throws SQLException {
        final String SELECT_SQL = "SELECT key_data FROM RSIN_" + rsin + " WHERE rowid=" + (index + 1);//Database is not zero indexed
        return db.createStatement().executeQuery(SELECT_SQL).getBytes(1);
    }
}
