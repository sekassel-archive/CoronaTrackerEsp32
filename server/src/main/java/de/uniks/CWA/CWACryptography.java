package de.uniks.CWA;

import at.favre.lib.crypto.HKDF;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class CWACryptography {
    private static final int ENIN_SIZE = 4;

    private static final String RPIK_INFO_STRING = "EN-RPIK";
    private static final byte[] RPIK_INFO = RPIK_INFO_STRING.getBytes(StandardCharsets.UTF_8);
    private static final int RPIK_LENGTH = 16;

    private static final String PADDED_DATA_INFO_STRING = "EN-RPI";
    private static final byte[] PADDED_DATA_INFO = PADDED_DATA_INFO_STRING.getBytes(StandardCharsets.UTF_8);
    private static final int PADDED_DATA_LENGTH = 16;
    private static final int PADDED_DATA_INFO_START = 0;
    private static final int PADDED_DATA_ENIN_START = 12;

    public static final String AES = "AES";
    public static final String RPI_CIPHER_TRANSFORMATION = "AES/ECB/NoPadding";

    public static final int TWO_WEEKS_IN_10_MINUTES_INTERVAL = 6 * 24 * 14;

    private static byte[] getENIntervalNumber(int timestamp) {
        return ByteBuffer.allocate(ENIN_SIZE).order(ByteOrder.LITTLE_ENDIAN).putInt(timestamp).array();
    }

    private static byte[] getENIntervalNumber(long timestamp) {
        return getENIntervalNumber((int) (timestamp / (60 * 10)));
    }

    private static byte[] getRollingProximityIdentifierKey(byte[] tek_i) {
        HKDF hkdf = HKDF.fromHmacSha256();
        return hkdf.expand(tek_i, RPIK_INFO, RPIK_LENGTH);
    }

    private static byte[] getPaddedData(int j) {
        byte[] paddedData = new byte[PADDED_DATA_LENGTH];
        System.arraycopy(PADDED_DATA_INFO, 0, paddedData, PADDED_DATA_INFO_START, PADDED_DATA_INFO.length);
        byte[] ENIN = getENIntervalNumber(j);
        System.arraycopy(ENIN, 0, paddedData, PADDED_DATA_ENIN_START, ENIN.length);
        return paddedData;
    }

    private static byte[] getRollingProximityIdentifier(byte[] RPIK, byte[] paddedData) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher c = Cipher.getInstance(RPI_CIPHER_TRANSFORMATION);
        SecretKeySpec key = new SecretKeySpec(RPIK, AES);

        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal(paddedData);
    }

    public static byte[] getRollingProximityIdentifier(byte[] tek, int j) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return getRollingProximityIdentifier(getRollingProximityIdentifierKey(tek), getPaddedData(j));
    }

    public static int getRollingStartIntervalNumber(long timestamp) {
        return (int) timestamp / (60 * 10);
    }
}
