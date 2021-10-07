package de.uniks.postgres.db;

import de.uniks.cwa.CwaDataInterpreter;
import de.uniks.postgres.db.model.InfectedUser;
import de.uniks.postgres.db.model.User;
import de.uniks.postgres.db.model.VerificationUser;
import de.uniks.postgres.db.utils.InfectedUserPostgreSql;
import de.uniks.postgres.db.utils.UserPostgreSql;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LocalDbConnectionTest {

    // change ip address for your local DB
    private static final String LOCAL_DB_URL = "jdbc:postgresql://192.168.5.3:5432/tracker";
    private Optional<Connection> connection;

    @BeforeAll
    public void establishLocalDbConnection() throws NoSuchFieldException, IllegalAccessException {
        // access private methods with reflections
        Class pgClass = PostgresConnect.class;

        // change private modifier to public
        Field privateField = pgClass.getDeclaredField("DB_URL");
        privateField.setAccessible(true);

        // get and set field value
        String old_value = (String) privateField.get(pgClass);
        privateField.set(pgClass, LOCAL_DB_URL);
        String new_value = (String) privateField.get(pgClass);

        // output just for display purpose
        System.out.println("changed DB_URL from:\n" + old_value + "\nto new DB_URL:\n" + new_value);

        // connect to local DB and make sure it is connected
        connection = PostgresConnect.getConnection();
        Assert.assertTrue(connection.isPresent());
    }

    @AfterAll
    public void closeLocalDbConnection() throws SQLException {
        // close connection to local DB
        connection.get().close();
    }

    @Test
    public void localDbAddAndDeleteUserTest() {
        // User test data set
        byte[] testRpi = {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2};
        User user = new User("3ee56edf-1e91-438f-b75e-50df9a30e515", 0, 2692512, List.of(testRpi));

        List<User> userBack;
        UserPostgreSql userInterface = new UserPostgreSql();

        Optional<Integer> returnOpt = userInterface.save(user);
        Assert.assertTrue(returnOpt.isPresent());
        Assert.assertFalse(returnOpt.isEmpty());
        Assert.assertEquals((Integer) 1, returnOpt.get());
        userBack = userInterface.get(user.getUuid());
        Assert.assertFalse(userBack.isEmpty());

        Assert.assertEquals(user.getUuid(), userBack.get(0).getUuid());
        Assert.assertEquals(user.getStatus(), userBack.get(0).getStatus());
        Assert.assertEquals(user.getEnin(), userBack.get(0).getEnin());
        Assert.assertTrue(user.getRpiListAsJSONArray().equals(userBack.get(0).getRpiListAsJSONArray()));
        Assert.assertEquals(1, user.getRpiList().size());
        Assert.assertEquals(1, userBack.get(0).getRpiList().size());
        Assert.assertTrue(Arrays.equals(user.getRpiList().get(0), (userBack.get(0).getRpiList().get(0))));

        userInterface.delete(user);
        userBack = userInterface.get(user.getUuid());
        Assert.assertTrue(userBack.isEmpty());
    }

    @Test
    public void runInfectionCheckOnLocalUserTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        UserPostgreSql userDb = new UserPostgreSql();
        InfectedUserPostgreSql infectedUserDb = new InfectedUserPostgreSql();
        ConcurrentHashMap<Integer, List<byte[]>> emptyCwaData = new ConcurrentHashMap<>();

        Collection<InfectedUser>  infectedUserList = infectedUserDb.getAllCompleteInfectedUser();

        // access private method with reflection
        Class cwaClass = CwaDataInterpreter.class;
        Method builderMethod = cwaClass.getDeclaredMethod("buildInfectedUserEninRpisMap", ConcurrentHashMap.class, Collection.class);
        builderMethod.setAccessible(true);

        ConcurrentHashMap<Integer, List<byte[]>> eninRpisMap =
                (ConcurrentHashMap<Integer, List<byte[]>>) builderMethod.invoke(null, emptyCwaData, infectedUserList);

        Assert.assertFalse(eninRpisMap.isEmpty());

        Method queryInfectionCheckMethod = cwaClass.getDeclaredMethod("buildAndQueryInfectionCheckOnDb", UserPostgreSql.class, ConcurrentHashMap.class);
        queryInfectionCheckMethod.setAccessible(true);

        Optional<List<User>> infectedUserActionRequired =
                (Optional<List<User>>) queryInfectionCheckMethod.invoke(null, userDb, eninRpisMap);

        if(infectedUserActionRequired.isEmpty()){
            System.out.println("\nNO INFECTED USER FOUND!\n");
        } else {
            System.out.println("\nINFECTED USER FOUND: " + infectedUserActionRequired.get().size() + "\n");
            infectedUserActionRequired.get().forEach(user -> {
                System.out.println("\nUUID:" + user.getUuid() + "\nstatus:" + user.getStatus() + "\nenin:" + user.getEnin() + "\n");
            });
        }
    }

    @Test
    public void printUserDB() {
        String sql = "SELECT * FROM " + User.CLASS + ";";
        List<User> users = new ArrayList<>();
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    String uuid = resultSet.getString(User.UUID);
                    Integer status = resultSet.getInt(User.STATUS);
                    Integer rsin = resultSet.getInt(User.ENIN);
                    String tekListAsJSONArray = resultSet.getString(User.RPILIST);
                    System.out.println("UUID:" + uuid + "\nstatus:" + status + "\nenin:" + rsin + "\nrpiList:" + tekListAsJSONArray + "\n\n");
                    users.add(new User(uuid, status, rsin, tekListAsJSONArray));
                }
            } catch (SQLException ex) {
                Assert.fail();
            }
        });
    }

    @Test
    public void printVerificationDB() {
        String sql = "SELECT * FROM " + VerificationUser.CLASS + ";";
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    String uuid = resultSet.getString(VerificationUser.UUID);
                    String pin = resultSet.getString(VerificationUser.PIN);
                    Boolean tokenActive = resultSet.getBoolean(VerificationUser.TOKEN_ACTIVE);
                    Boolean tokenValid = resultSet.getBoolean(VerificationUser.TOKEN_VALID);
                    String timestamp = resultSet.getString(VerificationUser.TIMESTAMP);
                    Boolean inputReadyForPickup = resultSet.getBoolean(VerificationUser.INPUT_READY_FOR_PICKUP);
                    Boolean userInfected = resultSet.getBoolean(VerificationUser.USER_INFECTED);
                    String rsin = resultSet.getString(VerificationUser.RSIN);
                    System.out.println("uuid:" + uuid + "\n" +
                            "pin:" + pin + "\n" +
                            "tokenActive:" + tokenActive.toString() + "\n" +
                            "tokenValid:" + tokenValid.toString() + "\n" +
                            "timestamp:" + timestamp + "\n" +
                            "inputReadyForPickup:" + inputReadyForPickup + "\n" +
                            "userInfected:" + userInfected + "\n" +
                            "rsin:" + rsin + "\n" +
                            "\n");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                Assert.fail();
            }
        });
    }

    @Test
    public void printInfectedUserDB() {
        String sql = "SELECT * FROM " + InfectedUser.CLASS + ";";
        connection.ifPresent(conn -> {
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    String uuid = resultSet.getString(InfectedUser.UUID);
                    String tek = resultSet.getString(InfectedUser.TEK);
                    String rsin = resultSet.getString(InfectedUser.RSIN);
                    System.out.println("uuid:" + uuid + "\n" +
                            "tek:" + tek + "\n" +
                            "rsin:" + rsin + "\n" +
                            "\n");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                Assert.fail();
            }
        });
    }

    //@Ignore
    @Test
    public void wipeLocalUserDB() {
        String sql = "TRUNCATE TABLE " + User.CLASS + ";";
        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                Assert.fail();
            }
        });
    }

    @Ignore
    @Test
    public void wipeLocalInfectedUserDB() {
        String sql = "TRUNCATE TABLE " + InfectedUser.CLASS + ";";
        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                Assert.fail();
            }
        });
    }

    @Ignore
    @Test
    public void wipeLocalUserVerificationDB() {
        String sql = "TRUNCATE TABLE " + VerificationUser.CLASS + ";";
        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                Assert.fail();
            }
        });
    }

    @Ignore
    @Test
    public void deleteLocalUserVerificationDB() {
        String sql = "DROP TABLE " + VerificationUser.CLASS + ";";
        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                Assert.fail();
            }
        });
    }

    @Ignore
    @Test
    public void deleteLocalInfectedUserDB() {
        String sql = "DROP TABLE " + InfectedUser.CLASS + ";";
        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                Assert.fail();
            }
        });
    }

    @Ignore
    @Test
    public void deleteLocalUserDB() {
        String sql = "DROP TABLE " + User.CLASS + ";";
        connection.ifPresent(conn -> {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                Assert.fail();
            }
        });
    }
}
