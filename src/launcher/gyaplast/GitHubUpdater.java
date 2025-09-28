/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package launcher.gyaplast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/**
 *
 * @author NOTEJUAN
 */
public class GitHubUpdater {

    private static final String API_URL = "https://api.github.com/repos/Desarrollador-LELLA/launcher-pedidos/releases/latest";
    private static String latestDownloadUrl = null;

    public static String fetchLatestVersion() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github+json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            JSONObject release = new JSONObject(json.toString());
            String latestVersion = release.getString("tag_name").replace("v", "");
            latestDownloadUrl = release.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
            return latestVersion;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getLatestDownloadUrl() {
        return latestDownloadUrl;
    }

    public static boolean isNewerVersion(String currentVersion, String latestVersion) {
        if (currentVersion == null) {
            return true;
        }

        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (latestPart > currentPart) {
                return true;
            }
            if (latestPart < currentPart) {
                return false;
            }
        }
        return false;
    }

}
