import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;
import javafx.scene.paint.Color;

public class CronometroTimer {

    private Label label;
    private Timeline timeline;
    private int segundos; // contador de segundos

    private boolean rodando;

    private Color corAtual = Color.YELLOW;

    public CronometroTimer(Label label) {
        this.label = label;
        segundos = 0;
        rodando = false;

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segundos++;
            atualizarLabel();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void start() {
        if (!rodando) {
            timeline.play();
            rodando = true;
        }
    }

    public void pause() {
        if (rodando) {
            timeline.pause();
            rodando = false;
        }
    }

    public void stop() {
        timeline.stop();
        rodando = false;
        segundos = 0;
        atualizarLabel();
    }

    public void reset() {
        segundos = 0;
        atualizarLabel();
    }

    public boolean isRodando() {
        return rodando;
    }

    private void atualizarLabel() {
        Platform.runLater(() -> {
            label.setText(getTempoFormatado());
            label.setTextFill(corAtual);
        });
    }

    public String getTempoFormatado() {
        int hrs = segundos / 3600;
        int min = (segundos % 3600) / 60;
        int seg = segundos % 60;
        return String.format("%02d:%02d:%02d", hrs, min, seg);
    }

    public void setCorAtual(Color cor) {
        this.corAtual = cor;
        Platform.runLater(() -> label.setTextFill(cor));
    }
}
