package de.uniks;

import de.uniks.proto.Exportkey;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CWARequests {

    private static final String infections_url = "https://svc90.main.px.t-online.de/version/v1/diagnosis-keys/country/DE/date/";
    private static final String dates_url = "https://svc90.main.px.t-online.de/version/v1/diagnosis-keys/country/DE/date";

    public static List<BigInteger> getAllInfectionKeys() throws IOException, InterruptedException {
        String[] dates = getInfectionDates();
        List<BigInteger> keys = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();

        for (String date : dates) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(infections_url + date))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            FileOutputStream fos = new FileOutputStream("response.zip");
            fos.write(response.body());
            fos.close();

            ZipFile zip = new ZipFile("response.zip");
            ZipEntry exportEntry = zip.getEntry("export.bin");

            byte[] bodyData = IOUtils.toByteArray(zip.getInputStream(exportEntry));

            zip.close();
            Files.delete(Paths.get("response.zip"));

            byte[] exportData = Arrays.copyOfRange(bodyData, 16, bodyData.length);

            Exportkey.TemporaryExposureKeyExport export = Exportkey.TemporaryExposureKeyExport.parseFrom(exportData);
            List<Exportkey.TemporaryExposureKey> keysList = export.getKeysList();

            for (Exportkey.TemporaryExposureKey key : keysList) {
                keys.add(new BigInteger(key.getKeyData().toByteArray()));
            }
        }
        return keys;
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
