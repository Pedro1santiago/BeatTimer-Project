import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;
import javafx.scene.paint.Color;

public class PomodoroTimer {

    public interface PomodoroListener {
        void onPomodoroFim();
        void onPausaFim();
    }

    private Label label;
    private Timeline timeline;

    private int segundosRestantes;
    private boolean rodando;
    private boolean emPausa;

    private Color corAtual = Color.YELLOW;

    private final int DURACAO_POMODORO = 25 * 60;
    private final int DURACAO_PAUSA = 5 * 60;

    private PomodoroListener listener;

    public PomodoroTimer(Label label) {
        this.label = label;
        this.segundosRestantes = DURACAO_POMODORO;
        this.rodando = false;
        this.emPausa = false;

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (segundosRestantes > 0) {
                segundosRestantes--;
                atualizarLabel();
            } else {
                timeline.stop();
                rodando = false;

                if (!emPausa) {
                    if (listener != null) listener.onPomodoroFim();
                } else {
                    if (listener != null) listener.onPausaFim();
                }
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void startPomodoro() {
        emPausa = false;
        if (!rodando) {
            timeline.play();
            rodando = true;
        }
    }

    public void iniciarPausa() {
        emPausa = true;
        segundosRestantes = DURACAO_PAUSA;
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
        segundosRestantes = DURACAO_POMODORO;
        emPausa = false;
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
        int min = segundosRestantes / 60;
        int seg = segundosRestantes % 60;
        return String.format("%02d:%02d", min, seg);
    }

    public void setCorAtual(Color cor) {
        this.corAtual = cor;
        Platform.runLater(() -> label.setTextFill(cor));
    }

    public void setPomodoroListener(PomodoroListener listener) {
        this.listener = listener;
    }
}
