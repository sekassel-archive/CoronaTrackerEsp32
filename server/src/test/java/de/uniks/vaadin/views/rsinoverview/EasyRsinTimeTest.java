package de.uniks.vaadin.views.rsinoverview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EasyRsinTimeTest {
    @Test
    public void showRsinTimeTest() {
        RsinEntrys entrys = new RsinEntrys(1234567, "2692575");//11:30
        String expectedDate = "12.03.2021";
        String s = entrys.getRsinDate();
        assertEquals(s,expectedDate);
    }
}
