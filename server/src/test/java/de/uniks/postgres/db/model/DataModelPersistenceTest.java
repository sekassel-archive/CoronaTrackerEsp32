package de.uniks.postgres.db.model;

import org.json.JSONArray;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataModelPersistenceTest {
    private User testUser1;
    private User testUser2;
    private User testUser3;
    private User testProd4;

    @BeforeEach
    public void buildTestData() {
        byte[] testByte1 = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        byte[] testByte2 = {2, -2, 2, -2, 2, -2, 2, -2, 2, -2, 2, -2, 2, -2, 2, -2};
        byte[] testByte3 = {-3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3};

        byte[] prodByte1 = {0, 10, 41, -77, 24, -50, 91, -59, -73, 77, 58, 16, -125, -123, -51, 25};
        byte[] prodByte2 = {0, 15, -10, 125, -61, 63, 88, 127, 29, 125, 7, 75, 23, -114, -52, 83};
        byte[] prodByte3 = {0, 21, -88, 111, -47, 23, -41, 74, 17, -48, -92, 112, 103, -119, -58, -94};

        List<byte[]> testRpiListUser1 = new ArrayList<>();
        List<byte[]> testRpiListUser2 = new ArrayList<>();
        List<byte[]> testRpiListUser3 = new ArrayList<>();
        List<byte[]> testRpiListProd4 = new ArrayList<>();

        testRpiListUser1.add(testByte1);
        testRpiListUser2.add(testByte2);
        testRpiListUser3.add(testByte3);
        testRpiListProd4.add(prodByte1);
        testRpiListProd4.add(prodByte2);
        testRpiListProd4.add(prodByte3);

        testUser1 = new User("3ee56edf-1e91-438f-b75e-50df9a30e515", 0, 1111111, testRpiListUser1);
        testUser2 = new User("112d6628-4a3d-49b7-b191-d882255af210", 0, 2222222, testRpiListUser2);
        testUser3 = new User("7d729331-44ce-47c7-b260-6035613218c8", 0, 3333333, testRpiListUser3);
        testProd4 = new User("a83a793f-7476-4b7c-850e-36212c3747cc", 2, 2692512, testRpiListProd4);
    }

    @Ignore
    @Test
    public void serializeAndDeserializeUserTest() {
        String expectedTestUser1RpiListAsJSONArrayString = new JSONArray(testUser1.getRpiList()).toString();// "[[1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1]]";
        String expectedTestUser2RpiListAsJSONArrayString = new JSONArray(testUser2.getRpiList()).toString();// "[[2,-2,2,-2,2,-2,2,-2,2,-2,2,-2,2,-2,2,-2]]";
        String expectedTestUser3RpiListAsJSONArrayString = new JSONArray(testUser3.getRpiList()).toString();// "[[-3,-3,-3,-3,-3,-3,-3,-3,-3,-3,-3,-3,-3,-3,-3,-3]]";
        String expectedTestProd4RpiListAsJSONArrayString = new JSONArray(testProd4.getRpiList()).toString();// "[[0,10,41,-77,24,-50,91,-59,-73,77,58,16,-125,-123,-51,25],[0,15,-10,125,-61,63,88,127,29,125,7,75,23,-114,-52,83],[0,21,-88,111,-47,23,-41,74,17,-48,-92,112,103,-119,-58,-94]]";
/*
        String resultTestUser1 = testUser1.getRpiListAsJSONArray();
        String resultTestUser2 = testUser2.getRpiListAsJSONArray();
        String resultTestUser3 = testUser3.getRpiListAsJSONArray();
        String resultTestProd4 = testProd4.getRpiListAsJSONArray();

        assertEquals(expectedTestUser1RpiListAsJSONArrayString, resultTestUser1);
        assertEquals(expectedTestUser2RpiListAsJSONArrayString, resultTestUser2);
        assertEquals(expectedTestUser3RpiListAsJSONArrayString, resultTestUser3);
        assertEquals(expectedTestProd4RpiListAsJSONArrayString, resultTestProd4);
*/
        User testUser1copy = new User(testUser1.getUuid(), testUser1.getStatus(), testUser1.getEnin(), expectedTestUser1RpiListAsJSONArrayString);
        User testUser2copy = new User(testUser2.getUuid(), testUser2.getStatus(), testUser2.getEnin(), expectedTestUser2RpiListAsJSONArrayString);
        User testUser3copy = new User(testUser3.getUuid(), testUser3.getStatus(), testUser3.getEnin(), expectedTestUser3RpiListAsJSONArrayString);
        User testProd4copy = new User(testProd4.getUuid(), testProd4.getStatus(), testProd4.getEnin(), expectedTestProd4RpiListAsJSONArrayString);

        assertTrue(Arrays.equals(testUser1.getRpiList().get(0), testUser1copy.getRpiList().get(0)));
        assertTrue(Arrays.equals(testUser2.getRpiList().get(0), testUser2copy.getRpiList().get(0)));
        assertTrue(Arrays.equals(testUser3.getRpiList().get(0), testUser3copy.getRpiList().get(0)));
        assertTrue(Arrays.equals(testProd4.getRpiList().get(0), testProd4copy.getRpiList().get(0)));
        assertTrue(Arrays.equals(testProd4.getRpiList().get(1), testProd4copy.getRpiList().get(1)));
        assertTrue(Arrays.equals(testProd4.getRpiList().get(2), testProd4copy.getRpiList().get(2)));
    }
}
