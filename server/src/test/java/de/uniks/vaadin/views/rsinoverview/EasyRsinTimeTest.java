package de.uniks.vaadin.views.rsinoverview;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EasyRsinTimeTest {
    @Test
    public void showRsinTimeTest() {
        RsinEntrys entrys = new RsinEntrys(1234567, "2692575");//11:30
        String expectedDate = "12.03.2021";
        String s = entrys.getRsinDate();
        assertEquals(s,expectedDate);

        System.out.println("Time: " + new Date(new Long( Integer.parseInt("2697408")) * 600 * 1000).toLocaleString());
        System.out.println("Time: " + new Date(new Long( Integer.parseInt("2697611")) * 600 * 1000).toLocaleString());
        System.out.println("Time: " + new Date(new Long( Integer.parseInt("2694155")) * 600 * 1000).toLocaleString());
    }
    @Test
    public void showTimeTest() {
        System.out.println("Time: " + new Date(new Long( Integer.parseInt("2702075")) * 600 * 1000).toLocaleString());
    }
}
