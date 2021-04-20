package de.uniks.cwa;

import de.uniks.cwa.utils.CWACryptography;
import de.uniks.postgres.db.model.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CwaDataInterpreterTest {

    private static CwaDataInterpreter cwaDataInterpreter = new CwaDataInterpreter();

    private static ConcurrentHashMap<Integer, List<byte[]>> cwaData;

    private User testUser1;
    private User testUser2;

    @Test
    public void cwaDataAndUserDataCompareTest() throws Exception {
        //cwaData = cwaDataInterpreter.getCwaData();
        cwaData.clear();
        //TODO: refactor
        Integer rsinProd1 = 2692512;
        List<byte[]> rsinProd1TestRpiList = new ArrayList<>();
        rsinProd1TestRpiList.add(new byte[]{0, 10, 41, -77, 24, -50, 91, -59, -73, 77, 58, 16, -125, -123, -51, 25});
        rsinProd1TestRpiList.add(new byte[]{0, 15, -10, 125, -61, 63, 88, 127, 29, 125, 7, 75, 23, -114, -52, 83});
        rsinProd1TestRpiList.add(new byte[]{0, 21, -88, 111, -47, 23, -41, 74, 17, -48, -92, 112, 103, -119, -58, -94});

        Integer rsinProd2 = 2695968;
        List<byte[]> rsinProd2TestRpiList = new ArrayList<>();
        rsinProd2TestRpiList.add(new byte[]{0, 72, 60, -84, 36, -102, 86, -30, -96, -98, -88, -106, 113, -73, -4, 69});
        rsinProd2TestRpiList.add(new byte[]{0, 97, 93, -117, -39, -60, 86, -89, 10, -56, -56, -45, 21, -112, 118, -118});
        rsinProd2TestRpiList.add(new byte[]{0, -95, 29, -54, 44, -100, -94, -87, 79, 92, -102, 23, 13, 28, 29, 64});

        cwaData.put(rsinProd1, rsinProd1TestRpiList);
        cwaData.put(rsinProd2, rsinProd2TestRpiList);

        // timestamp should be between 0 - 144
        byte[] sharedInfectedRpi = CWACryptography.getRollingProximityIdentifier(rsinProd2TestRpiList.get(1), rsinProd2 + 1);

        byte[] testRpi1 = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        byte[] testRpi2 = {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2};

        testUser1 = new User("3ee56edf-1e91-438f-b75e-50df9a30e515", 0, 2692512, List.of(testRpi2));
        // new User("3ee56edf-1e91-438f-b75e-50df9a30e515", 0, 2695968, List.of(testRpi2));
        // new User("3ee56edf-1e91-438f-b75e-50df9a30e515", 0, 2696112, List.of(testRpi2));

        testUser2 = new User("112d6628-4a3d-49b7-b191-d882255af210", 0, 2692512, List.of(testRpi1, sharedInfectedRpi));
        // new User("112d6628-4a3d-49b7-b191-d882255af210", 0, 2695968, List.of(testRpi1));
        // new User("112d6628-4a3d-49b7-b191-d882255af210", 0, 2696112, List.of(testRpi1));

        // access private methods with reflections
        Class cwaDI = CwaDataInterpreter.class;
        Method privateMethod = cwaDI.getDeclaredMethod("processInfectionCheck", Collection.class);
        privateMethod.setAccessible(true);
        privateMethod.invoke(cwaDataInterpreter, List.of(testUser1,testUser2));

        //TODO: validate true and false found new infections
    }
}
