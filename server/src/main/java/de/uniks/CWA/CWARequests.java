package de.uniks.CWA;

import de.uniks.proto.Exportkey;
import javafx.util.Pair;
import org.json.JSONArray;
import spark.utils.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CWARequests {

    private static final String dates_url = "https://svc90.main.px.t-online.de/version/v1/diagnosis-keys/country/DE/date";
    private static final String infections_url = dates_url + "/";
    private static final String ZIP_TEMPORARY = "response.zip";
    private static final String EXPORT_BIN = "export.bin";

    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static Map<Long, List<BigInteger>> getAllInfectionKeys() throws IOException, InterruptedException {
        String[] dates = getInfectionDates();
        Map<Long, List<BigInteger>> keyMap = new HashMap<>();

        for (String date : dates) {
            Pair<Long, List<BigInteger>> keys = getInfectionKeys(date);
            keyMap.put(keys.getKey(), keys.getValue());
        }
        return keyMap;
    }

    private static Pair<Long, List<BigInteger>> getInfectionKeys(String date) throws DateTimeParseException, IOException, InterruptedException {
        LocalDate.parse(date, dateFormatter);
        List<BigInteger> keys = new ArrayList<>();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(infections_url + date))
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        FileOutputStream fos = new FileOutputStream(ZIP_TEMPORARY);
        fos.write(response.body());
        fos.close();

        ZipFile zip = new ZipFile(ZIP_TEMPORARY);
        ZipEntry exportEntry = zip.getEntry(EXPORT_BIN);

        byte[] bodyData = IOUtils.toByteArray(zip.getInputStream(exportEntry));

        zip.close();
        Files.delete(Paths.get(ZIP_TEMPORARY));

        byte[] exportData = Arrays.copyOfRange(bodyData, 16, bodyData.length);

        Exportkey.TemporaryExposureKeyExport export = Exportkey.TemporaryExposureKeyExport.parseFrom(exportData);
        List<Exportkey.TemporaryExposureKey> keysList = export.getKeysList();

        for (Exportkey.TemporaryExposureKey key : keysList) {
            keys.add(new BigInteger(key.getKeyData().toByteArray()));
        }
        return new Pair<>(export.getStartTimestamp(), keys);
    }

    private static String[] getInfectionDates() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dates_url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Request returned with status code " + response.statusCode());
        }

        JSONArray responseAsJson = new JSONArray(response.body());
        String[] responseArray = new String[responseAsJson.length()];

        for (int i = 0; i < responseAsJson.length(); i++) {
            responseArray[i] = responseAsJson.getString(i);
        }

        return responseArray;
    }
}
