package de.uniks.payload;

import org.junit.Test;

import static org.junit.Assert.*;

public class InfectionPostPayloadTest {

    @Test
    public void isAuthenticated() {
        InfectionPostPayload ipp = new InfectionPostPayload();
        assertFalse(ipp.isAuthenticated());

        ipp.setAuthData(1234);
        assertTrue(ipp.isAuthenticated());
    }

    @Test
    public void isValid() {
        InfectionPostPayload ipp = new InfectionPostPayload();
        assertFalse(ipp.isValid());

        ipp.setTime(100);
        ipp.setId(1);
        assertFalse(ipp.isValid());

        ipp.setTime((System.currentTimeMillis()/1000L) + 2000);
        assertFalse(ipp.isValid());

        ipp.setTime(1590969600);
        ipp.setId(1);
        assertTrue(ipp.isValid());
    }

    @Test
    public void getAuthData() {
        InfectionPostPayload ipp = new InfectionPostPayload();
        assertEquals(0, ipp.getAuthData());
    }

    @Test
    public void setAuthData() {
        InfectionPostPayload ipp = new InfectionPostPayload();
        ipp.setAuthData(1234);
        assertEquals(1234, ipp.getAuthData());
    }

    @Test
    public void getTime() {
        InfectionPostPayload ipp = new InfectionPostPayload();
        assertEquals(0, ipp.getTime());
    }

    @Test
    public void getId() {
        InfectionPostPayload ipp = new InfectionPostPayload();
        assertEquals(0, ipp.getId());
    }

    @Test
    public void setTime() {
        InfectionPostPayload ipp = new InfectionPostPayload();
        ipp.setTime(100);
        assertEquals(100, ipp.getTime());
    }

    @Test
    public void setId() {
        InfectionPostPayload ipp = new InfectionPostPayload();
        ipp.setId(100);
        assertEquals(100, ipp.getId());
    }
}
