package de.uniks.postgres.db;

import de.uniks.postgres.db.model.User;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
    public void localDbConnectionTest() {
        // User test data set
        byte[] testRpi = {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2};
        User user = new User("3ee56edf-1e91-438f-b75e-50df9a30e515", 0, 2692512, List.of(testRpi));

        List<User> userBack;
        UserPostgreSqlDao userInterface = new UserPostgreSqlDao();

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
}
