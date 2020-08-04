package de.uniks.CWA;

import de.uniks.proto.Exportkey;
import org.json.JSONArray;
import spark.utils.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
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

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    //Returns List of all RollingStartIntervalNumbers with their TemporaryExposureKeys
    public static Map<Integer, List<byte[]>> getUnzippedInfectionData() throws IOException, InterruptedException {
        String[] dates = getInfectionDates();
        Map<Integer, List<byte[]>> keyMap = new HashMap<>();

        for (String date : dates) {
            Map<Integer, List<byte[]>> data = getInfectionData(date);

            for (Map.Entry<Integer, List<byte[]>> entry : data.entrySet()) {
                if(!keyMap.containsKey(entry.getKey())) {
                    keyMap.put(entry.getKey(), new ArrayList<>());
                }
                keyMap.get(entry.getKey()).addAll(entry.getValue());
            }
        }
        return keyMap;
    }

    private static Map<Integer, List<byte[]>> getInfectionData(String date) throws DateTimeParseException, IOException, InterruptedException {
        LocalDate.parse(date, dateFormatter);

        Map<Integer, List<byte[]>> infectionData = new HashMap<>();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(infections_url + date))
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Request returned with status code " + response.statusCode());
        }

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
            int rsin = key.getRollingStartIntervalNumber();

            if (!infectionData.containsKey(rsin)) {
                infectionData.put(rsin, new ArrayList<>());
            }
            infectionData.get(rsin).add(key.getKeyData().toByteArray());
        }
        return infectionData;
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
