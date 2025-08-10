import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class RelogioConfig extends Application {

    private Label relogioLabel, cronometroLabel, pomodoroLabel;
    private CronometroTimer cronometro;
    private PomodoroTimer pomodoro;

    private Color corAtual = Color.YELLOW;

    private TextField barraPesquisaTopo;
    private Stage stage;
    private Scene cena;
    private ColorPicker seletorCor;

    private Button btnIniciarCronometro, btnPausarCronometro, btnReiniciarCronometro, btnVoltarCronometro;
    private Button btnIniciarPomodoro, btnPararPomodoro, btnVoltarPomodoro;
    private Button btnCronometro, btnPomodoro, btnLogarSpotify;

    private SpotifyAuthenticator spotifyAuthenticator;
    private SpotifyService spotifyService;

    private Timeline relogioTimeline;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setFullScreen(true);

        relogioLabel = new Label();
        cronometroLabel = new Label("00:00:00");
        pomodoroLabel = new Label("25:00");

        btnIniciarCronometro = new Button("Iniciar");
        btnIniciarPomodoro = new Button("Iniciar");

        cronometro = new CronometroTimer(cronometroLabel);
        cronometro.setCorAtual(corAtual);

        pomodoro = new PomodoroTimer(pomodoroLabel);
        pomodoro.setCorAtual(corAtual);

        barraPesquisaTopo = new TextField();
        barraPesquisaTopo.setPromptText("Digite o nome da música e pressione Enter");
        barraPesquisaTopo.setPrefWidth(300);
        barraPesquisaTopo.setStyle(
                "-fx-background-color: black;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.5);" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-font-size: 16px;" +
                        "-fx-padding: 8;"
        );

        seletorCor = new ColorPicker(corAtual);
        atualizarEstiloSeletorCor();

        seletorCor.setOnAction(e -> {
            corAtual = seletorCor.getValue();
            atualizarCores();
            atualizarEstiloSeletorCor();
            if (cronometro != null) cronometro.setCorAtual(corAtual);
            if (pomodoro != null) pomodoro.setCorAtual(corAtual);
        });

        btnCronometro = new Button("Cronômetro");
        btnPomodoro = new Button("Pomodoro");
        estilizarBotao(btnCronometro);
        estilizarBotao(btnPomodoro);

        atualizarCores();

        btnCronometro.setOnAction(e -> configurarTelaCronometro());
        btnPomodoro.setOnAction(e -> configurarTelaPomodoro());

        spotifyAuthenticator = new SpotifyAuthenticator();

        btnLogarSpotify = new Button("Logar no Spotify");
        btnLogarSpotify.setStyle(
                "-fx-background-color: #1DB954;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-border-radius: 20px;" +
                        "-fx-padding: 8 20;"
        );

        btnLogarSpotify.setOnAction(e -> {
            new Thread(() -> {
                try {
                    CompletableFuture<String> futuroCodigo = spotifyAuthenticator.iniciarServidorEabrirNavegador();

                    futuroCodigo.thenCompose(codigo -> spotifyAuthenticator.solicitarTokenDeAcesso(codigo))
                            .thenAccept(respostaToken -> {
                                spotifyAuthenticator.processarRespostaToken(respostaToken);
                                spotifyService = new SpotifyService(spotifyAuthenticator);
                                Platform.runLater(() -> btnLogarSpotify.setText("Conectado"));
                            }).exceptionally(ex -> {
                                ex.printStackTrace();
                                return null;
                            }).get();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        cena = new Scene(criarLayoutPrincipal(), 1000, 700);
        stage.setScene(cena);
        stage.setTitle("BeatTimer");
        stage.show();

        configurarTelaRelogio();

        barraPesquisaTopo.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String texto = barraPesquisaTopo.getText().trim();
                if (!texto.isEmpty()) {
                    new Thread(() -> {
                        if (spotifyService != null && spotifyAuthenticator.estaAutenticado()) {
                            spotifyService.buscarETocarMusica(texto);
                        } else {
                            abrirNoSpotifyUrlPeloNome(texto);
                        }
                        Platform.runLater(() -> {
                            stage.toFront();
                            stage.requestFocus();
                        });
                    }).start();
                }
            }
        });
    }

    private void atualizarEstiloSeletorCor() {
        String corHex = toHex(corAtual);
        seletorCor.setStyle(
                "-fx-color-label-visible: false;" +
                        "-fx-padding: 0;" +
                        "-fx-pref-width: 40px;" +
                        "-fx-pref-height: 40px;" +
                        "-fx-background-color: " + corHex + ";" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-color: transparent;"
        );
        Platform.runLater(() -> {
            Node colorBox = seletorCor.lookup(".color-picker-color");
            if (colorBox != null) {
                colorBox.setStyle(
                        "-fx-background-color: " + corHex + ";" +
                                "-fx-background-radius: 3;"
                );
            }
        });
    }

    private void abrirNoSpotifyUrlPeloNome(String nomeMusica) {
        try {
            String query = java.net.URLEncoder.encode(nomeMusica, "UTF-8");
            String url = "https://open.spotify.com/search/" + query;
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                System.out.println("Não foi possível abrir navegador. Link: " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BorderPane criarLayoutPrincipal() {
        HBox direita = new HBox(btnLogarSpotify);
        direita.setAlignment(Pos.CENTER_RIGHT);
        direita.setPadding(new Insets(10));
        direita.setSpacing(10);

        VBox esquerda = new VBox(seletorCor);
        esquerda.setAlignment(Pos.TOP_CENTER);
        esquerda.setPadding(new Insets(10));
        esquerda.setSpacing(19);

        VBox pesquisaBox = new VBox(barraPesquisaTopo);
        pesquisaBox.setAlignment(Pos.CENTER);
        pesquisaBox.setPadding(new Insets(10));

        BorderPane barra = new BorderPane();
        barra.setLeft(esquerda);
        barra.setCenter(pesquisaBox);
        barra.setRight(direita);
        barra.setStyle("-fx-background-color: black;");

        VBox topoBox = new VBox(barra);
        topoBox.setPadding(new Insets(10));
        topoBox.setStyle("-fx-background-color: black;");

        relogioLabel.setStyle("-fx-font-size: 180px; -fx-text-fill: " + toHex(corAtual) + ";");
        VBox centro = new VBox(relogioLabel);
        centro.setAlignment(Pos.CENTER);
        centro.setPadding(new Insets(10));
        centro.setStyle("-fx-background-color: black;");

        HBox botoes = new HBox(15, btnCronometro, btnPomodoro);
        botoes.setAlignment(Pos.CENTER);
        botoes.setPadding(new Insets(20));
        botoes.setStyle("-fx-background-color: black;");

        BorderPane root = new BorderPane();
        root.setTop(topoBox);
        root.setCenter(centro);
        root.setBottom(botoes);
        root.setStyle("-fx-background-color: black;");

        return root;
    }

    private void configurarTelaRelogio() {
        if (cronometro != null) cronometro.pause();
        if (pomodoro != null) pomodoro.pause();

        relogioLabel.setStyle("-fx-font-size: 180px; -fx-text-fill: " + toHex(corAtual) + ";");

        if (relogioTimeline != null) relogioTimeline.stop();

        relogioTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            relogioLabel.setText(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        }));
        relogioTimeline.setCycleCount(Timeline.INDEFINITE);
        relogioTimeline.play();

        VBox centro = new VBox(relogioLabel);
        centro.setAlignment(Pos.CENTER);
        centro.setPadding(new Insets(10));
        centro.setStyle("-fx-background-color: black;");

        BorderPane root = (BorderPane) cena.getRoot();
        root.setCenter(centro);
    }

    private void configurarTelaCronometro() {
        btnIniciarCronometro.setText(cronometro.isRodando() ? "Executando" : "Iniciar");

        btnPausarCronometro = new Button("Pausar");
        btnReiniciarCronometro = new Button("Zerar");
        btnVoltarCronometro = new Button("<");

        estilizarBotao(btnIniciarCronometro);
        estilizarBotao(btnPausarCronometro);
        estilizarBotao(btnReiniciarCronometro);
        estilizarBotao(btnVoltarCronometro);

        btnIniciarCronometro.setOnAction(e -> {
            cronometro.start();
            btnIniciarCronometro.setText("Executando");
        });

        btnPausarCronometro.setOnAction(e -> {
            cronometro.pause();
            btnIniciarCronometro.setText("Continuar");
        });

        btnReiniciarCronometro.setOnAction(e -> {
            cronometro.reset();
            btnIniciarCronometro.setText("Iniciar");
        });

        btnVoltarCronometro.setOnAction(e -> {
            cena.setRoot(criarLayoutPrincipal());
            configurarTelaRelogio();
        });

        HBox botoes = new HBox(10, btnIniciarCronometro, btnPausarCronometro, btnReiniciarCronometro);
        botoes.setAlignment(Pos.CENTER);
        botoes.setPadding(new Insets(20));
        botoes.setStyle("-fx-background-color: black;");

        cronometroLabel.setText(cronometro.getTempoFormatado());
        cronometroLabel.setStyle("-fx-font-size: 180px; -fx-text-fill: " + toHex(corAtual) + ";");

        VBox centro = new VBox(20, cronometroLabel);
        centro.setAlignment(Pos.CENTER);
        centro.setStyle("-fx-background-color: black;");

        BorderPane layout = new BorderPane();
        layout.setTop(criarBarraSuperior(btnVoltarCronometro));
        layout.setCenter(centro);
        layout.setBottom(botoes);
        layout.setStyle("-fx-background-color: black;");

        cena.setRoot(layout);
    }

    private void configurarTelaPomodoro() {
        btnIniciarPomodoro.setText(pomodoro.isRodando() ? "Executando" : "Iniciar");

        btnPararPomodoro = new Button("Parar");
        btnVoltarPomodoro = new Button("<");

        estilizarBotao(btnIniciarPomodoro);
        estilizarBotao(btnPararPomodoro);
        estilizarBotao(btnVoltarPomodoro);

        pomodoro.setPomodoroListener(new PomodoroTimer.PomodoroListener() {
            @Override
            public void onPomodoroFim() {
                Platform.runLater(() -> btnIniciarPomodoro.setText("Iniciar Pausa"));
            }
            @Override
            public void onPausaFim() {
                Platform.runLater(() -> btnIniciarPomodoro.setText("Iniciar"));
            }
        });

        btnIniciarPomodoro.setOnAction(e -> {
            String texto = btnIniciarPomodoro.getText();
            switch (texto) {
                case "Iniciar":
                    pomodoro.startPomodoro();
                    btnIniciarPomodoro.setText("Executando");
                    break;
                case "Executando":
                    pomodoro.pause();
                    btnIniciarPomodoro.setText("Continuar");
                    break;
                case "Continuar":
                    pomodoro.startPomodoro();
                    btnIniciarPomodoro.setText("Executando");
                    break;
                case "Iniciar Pausa":
                    pomodoro.iniciarPausa();
                    btnIniciarPomodoro.setText("Executando Pausa");
                    break;
                case "Executando Pausa":
                    pomodoro.pause();
                    btnIniciarPomodoro.setText("Continuar Pausa");
                    break;
                case "Continuar Pausa":
                    pomodoro.startPomodoro();
                    btnIniciarPomodoro.setText("Executando Pausa");
                    break;
            }
        });

        btnPararPomodoro.setOnAction(e -> {
            pomodoro.stop();
            btnIniciarPomodoro.setText("Iniciar");
        });

        btnVoltarPomodoro.setOnAction(e -> {
            pomodoro.pause();
            cena.setRoot(criarLayoutPrincipal());
            configurarTelaRelogio();
        });

        pomodoroLabel.setText(pomodoro.getTempoFormatado());
        pomodoroLabel.setStyle("-fx-font-size: 180px; -fx-text-fill: " + toHex(corAtual) + ";");

        HBox botoes = new HBox(10, btnIniciarPomodoro, btnPararPomodoro);
        botoes.setAlignment(Pos.CENTER);
        botoes.setPadding(new Insets(20));
        botoes.setStyle("-fx-background-color: black;");

        VBox centro = new VBox(20, pomodoroLabel);
        centro.setAlignment(Pos.CENTER);
        centro.setStyle("-fx-background-color: black;");

        BorderPane layout = new BorderPane();
        layout.setTop(criarBarraSuperior(btnVoltarPomodoro));
        layout.setCenter(centro);
        layout.setBottom(botoes);
        layout.setStyle("-fx-background-color: black;");

        cena.setRoot(layout);
    }

    private BorderPane criarBarraSuperior(Button btnVoltar) {
        HBox esquerda = new HBox();
        esquerda.setAlignment(Pos.CENTER_LEFT);
        esquerda.setPadding(new Insets(10));
        esquerda.setSpacing(10);

        if (btnVoltar != null) {
            estilizarBotao(btnVoltar);
            esquerda.getChildren().addAll(btnVoltar, seletorCor);
        } else {
            esquerda.getChildren().add(seletorCor);
        }

        VBox pesquisaBox = new VBox(barraPesquisaTopo);
        pesquisaBox.setAlignment(Pos.CENTER);
        pesquisaBox.setPadding(new Insets(10));

        HBox direita = new HBox(btnLogarSpotify);
        direita.setAlignment(Pos.CENTER_RIGHT);
        direita.setPadding(new Insets(10));
        direita.setSpacing(10);

        BorderPane barra = new BorderPane();
        barra.setLeft(esquerda);
        barra.setCenter(pesquisaBox);
        barra.setRight(direita);
        barra.setStyle("-fx-background-color: black;");

        return barra;
    }

    private void atualizarCores() {
        String hex = toHex(corAtual);

        relogioLabel.setStyle("-fx-font-size: 180px; -fx-text-fill: " + hex + ";");
        cronometroLabel.setStyle("-fx-font-size: 180px; -fx-text-fill: " + hex + ";");
        pomodoroLabel.setStyle("-fx-font-size: 180px; -fx-text-fill: " + hex + ";");

        estilizarBotao(btnIniciarCronometro);
        estilizarBotao(btnPausarCronometro);
        estilizarBotao(btnReiniciarCronometro);
        estilizarBotao(btnVoltarCronometro);

        estilizarBotao(btnIniciarPomodoro);
        estilizarBotao(btnPararPomodoro);
        estilizarBotao(btnVoltarPomodoro);

        estilizarBotao(btnCronometro);
        estilizarBotao(btnPomodoro);

        barraPesquisaTopo.setStyle(
                "-fx-background-color: black;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.5);" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-font-size: 16px;" +
                        "-fx-padding: 8;"
        );
    }

    private void estilizarBotao(Button botao) {
        if (botao == null) return;
        String corHex = toHex(corAtual);
        botao.setStyle(
                "-fx-text-fill: " + corHex + ";" +
                        "-fx-background-color: black;" +
                        "-fx-font-size: 20px;" +
                        "-fx-border-color: " + corHex + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-padding: 10 20;"
        );
        botao.setOnMouseEntered(e ->
                botao.setStyle(
                        "-fx-text-fill: black;" +
                                "-fx-background-color: " + corHex + ";" +
                                "-fx-font-size: 20px;" +
                                "-fx-border-color: " + corHex + ";" +
                                "-fx-border-width: 2px;" +
                                "-fx-padding: 10 20;"
                )
        );
        botao.setOnMouseExited(e ->
                botao.setStyle(
                        "-fx-text-fill: " + corHex + ";" +
                                "-fx-background-color: black;" +
                                "-fx-font-size: 20px;" +
                                "-fx-border-color: " + corHex + ";" +
                                "-fx-border-width: 2px;" +
                                "-fx-padding: 10 20;"
                )
        );
    }

    private String toHex(Color cor) {
        return String.format("#%02X%02X%02X",
                (int)(cor.getRed() * 255),
                (int)(cor.getGreen() * 255),
                (int)(cor.getBlue() * 255));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
