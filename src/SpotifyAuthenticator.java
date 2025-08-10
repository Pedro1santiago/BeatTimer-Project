import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SpotifyAuthenticator  {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri = "http://127.0.0.1:8888/callback";

    private HttpServer servidor;
    private CompletableFuture<String> futuroCodigo;

    private String tokenAcesso;
    private String tokenRefresh;

    public SpotifyAuthenticator () {
        this.clientId = "bb70547c288e4eda9ba91f7f9c12f588";
        this.clientSecret = "c6aec55c3a2142c687b6ffbf95db8928";

        if (clientId == null || clientSecret == null) {
            throw new IllegalStateException("clientId ou clientSecret não definidos.");
        }
    }

    // Inicia servidor local e abre navegador para autenticar usuário no Spotify
    public CompletableFuture<String> iniciarServidorEabrirNavegador() throws IOException {
        futuroCodigo = new CompletableFuture<>();

        iniciarServidor();

        abrirAutorizacaoNoNavegador();

        return futuroCodigo;
    }

    private void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(8888), 0);
        servidor.createContext("/callback", new ManipuladorCallback());
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("Servidor iniciado em " + redirectUri);
    }

    private void abrirAutorizacaoNoNavegador() throws IOException {
        String scopes = URLEncoder.encode(
                "user-read-private user-read-email user-modify-playback-state user-read-playback-state streaming",
                StandardCharsets.UTF_8);
        String url = "https://accounts.spotify.com/authorize" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&scope=" + scopes +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI.create(url));
        } else {
            System.out.println("Por favor, abra esta URL no navegador:\n" + url);
        }
    }

    // Troca código de autorização pelo token de acesso e refresh token
    public CompletableFuture<String> solicitarTokenDeAcesso(String codigo) {
        HttpClient cliente = HttpClient.newHttpClient();

        String form = "grant_type=authorization_code" +
                "&code=" + URLEncoder.encode(codigo, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create("https://accounts.spotify.com/api/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + codificarBase64(clientId + ":" + clientSecret))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        return cliente.sendAsync(requisicao, HttpResponse.BodyHandlers.ofString())
                .thenApply(resposta -> {
                    servidor.stop(1); // para servidor após resposta do token
                    if (resposta.statusCode() == 200) {
                        return resposta.body();
                    } else {
                        throw new RuntimeException("Erro na requisição do token: " + resposta.body());
                    }
                });
    }

    /**
     * Processa JSON de resposta do token, armazenando os tokens.
     */
    public void processarRespostaToken(String corpoJson) {
        this.tokenAcesso = extrairValorJson(corpoJson, "access_token");
        this.tokenRefresh = extrairValorJson(corpoJson, "refresh_token");
        System.out.println("[AutenticadorSpotify] Token recebido: " + tokenAcesso);
    }

    private String extrairValorJson(String json, String campo) {
        String padrao = "\"" + campo + "\":\"";
        int inicio = json.indexOf(padrao);
        if (inicio == -1) return null;
        inicio += padrao.length();
        int fim = json.indexOf("\"", inicio);
        if (fim == -1) return null;
        return json.substring(inicio, fim);
    }

    private static String codificarBase64(String s) {
        return java.util.Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Retorna o token de acesso atual.
     */
    public String obterTokenAcesso() {
        return tokenAcesso;
    }

    /**
     * Verifica se está autenticado.
     */
    public boolean estaAutenticado() {
        return tokenAcesso != null && !tokenAcesso.isEmpty();
    }

    public CompletableFuture<String> obterFuturoCodigo() {
        return futuroCodigo;
    }

    // Recebe callback do Spotify com código de autorização
    class ManipuladorCallback implements HttpHandler {
        @Override
        public void handle(HttpExchange troca) throws IOException {
            String query = troca.getRequestURI().getQuery();
            Map<String, String> parametros = analisarQuery(query);
            String codigo = parametros.get("code");

            String respostaTexto;
            if (codigo != null) {
                futuroCodigo.complete(codigo);
                respostaTexto = "Autenticação bem-sucedida! Pode fechar esta janela.";
            } else {
                respostaTexto = "Falha na autenticação!";
            }

            byte[] bytes = respostaTexto.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            troca.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = troca.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static Map<String, String> analisarQuery(String query) {
        if (query == null || query.isEmpty()) return Map.of();
        return java.util.Arrays.stream(query.split("&"))
                .map(s -> s.split("=", 2))
                .filter(arr -> arr.length == 2)
                .collect(Collectors.toMap(
                        arr -> arr[0],
                        arr -> java.net.URLDecoder.decode(arr[1], StandardCharsets.UTF_8)
                ));
    }
}
