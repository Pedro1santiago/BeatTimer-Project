import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SpotifyService {

    private final SpotifyAuthenticator authenticator;

    public SpotifyService(SpotifyAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    private boolean isAuthenticated() {
        return authenticator != null && authenticator.estaAutenticado();
    }

    private String getAccessToken() {
        return authenticator.obterTokenAcesso();
    }

    public void buscarETocarMusica(String nomeMusica) {
        if (!isAuthenticated()) {
            System.out.println("⚠ Por favor, autentique-se primeiro.");
            return;
        }

        try {
            // 1. Buscar música
            String query = URLEncoder.encode(nomeMusica, StandardCharsets.UTF_8);
            String urlStr = "https://api.spotify.com/v1/search?q=" + query + "&type=track&limit=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

            if (conn.getResponseCode() == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                }

                JSONObject json = new JSONObject(response.toString());
                JSONArray tracks = json.getJSONObject("tracks").getJSONArray("items");

                if (tracks.length() > 0) {
                    JSONObject track = tracks.getJSONObject(0);
                    String trackName = track.getString("name");
                    String artistName = track.getJSONArray("artists").getJSONObject(0).getString("name");
                    String trackUrl = track.getJSONObject("external_urls").getString("spotify");
                    String trackUri = track.getString("uri");

                    System.out.println("🎵 Música encontrada: " + trackName + " - " + artistName);

                    // 2. Tentar tocar no dispositivo ativo
                    String deviceId = getActiveDeviceId();
                    if (deviceId != null) {
                        if (tocarMusica(trackUri, deviceId)) {
                            return;
                        }
                    }

                    // 3. Fallback: abrir no navegador/app
                    abrirNoSpotify(trackUrl);

                } else {
                    System.out.println("Nenhuma música encontrada.");
                }

            } else {
                System.err.println("Falha na busca: HTTP " + conn.getResponseCode());
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar música:");
            e.printStackTrace();
        }
    }

    private String getActiveDeviceId() {
        try {
            URL url = new URL("https://api.spotify.com/v1/me/player/devices");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

            if (conn.getResponseCode() == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                }

                JSONObject json = new JSONObject(response.toString());
                JSONArray devices = json.getJSONArray("devices");

                for (int i = 0; i < devices.length(); i++) {
                    JSONObject device = devices.getJSONObject(i);
                    if (device.getBoolean("is_active")) {
                        return device.getString("id");
                    }
                }

                // Se nenhum ativo, retorna o primeiro
                if (devices.length() > 0) {
                    return devices.getJSONObject(0).getString("id");
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao obter dispositivos:");
            e.printStackTrace();
        }
        return null;
    }

    private boolean tocarMusica(String trackUri, String deviceId) {
        try {
            String endpoint = "https://api.spotify.com/v1/me/player/play?device_id=" + deviceId;
            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonBody = "{ \"uris\": [\"" + trackUri + "\"] }";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 204) {
                System.out.println(" Música iniciada no Spotify!");
                return true;
            } else {
                System.err.println(" Falha ao tocar música: HTTP " + responseCode);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Erro ao tocar música:");
            e.printStackTrace();
            return false;
        }
    }

    private void abrirNoSpotify(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("Abrindo no Spotify...");
            } else {
                System.out.println("Não foi possível abrir o Spotify automaticamente. Link: " + url);
            }
        } catch (Exception e) {
            System.err.println("Erro ao abrir Spotify:");
            e.printStackTrace();
        }
    }
}
