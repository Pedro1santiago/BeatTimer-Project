# BeatTimer

**BeatTimer** é um aplicativo desktop elegante e funcional desenvolvido em **JavaFX**, que combina três ferramentas essenciais para produtividade e controle do tempo:

- **Relógio Digital** em tempo real com visual limpo e personalizável.
- **Cronômetro** com controles de iniciar, pausar e resetar.
- **Pomodoro Timer** para gerenciar ciclos de trabalho e pausas com notificações.

Além disso, o BeatTimer integra autenticação com o **Spotify**, permitindo buscar músicas.

---

## Funcionalidades principais

- Visual moderno e personalizável com escolha de cores.
- Cronômetro com controle completo: iniciar, pausar, reiniciar e visualizar tempo formatado.
- Pomodoro Timer configurável com ciclos de trabalho e pausas, incluindo alertas visuais.
- Integração com Spotify para login via OAuth
- Pesquisa rápida de músicas, abrindo no Spotify.
- Tela fullscreen com interface intuitiva.
- Barra de pesquisa para músicas com atalho Enter para busca.

---

## Tecnologias utilizadas

- Java 17
- JavaFX (interface gráfica)
- API do Spotify para autenticação 
- HTTP Server embutido para tratamento de callback OAuth
- JSON org.json para parsing de respostas

---

## Como executar

#Instale o executável

- Acesse https://github.com/Pedro1santiago/BeatTimer-Rel-gio-Cron-metro-Pomodoro/blob/master/Exec%20-%20BeatTimer.zip
- Clique em View raw.
- Abra o arquivo e execute o instalador.
- Após instalar o aplicativo, pesquise por "Relogio" e abra o aplicativo.


# Git clone -> 

git clone https://github.com/Pedro1santiago/BeatTimer-Rel-gio-Cron-metro-Pomodoro.git && cd BeatTimer-Rel-gio-Cron-metro-Pomodoro

# Compile os arquivos Java ->

- (substitua path_to_javafx_libs pelo caminho correto da sua lib JavaFX)

- No Windows:
javac -cp ".;path_to_javafx_libs/*" src/*.java

- No Linux/Mac:
javac -cp ".:path_to_javafx_libs/*" src/*.java

- Execute o app

-  No Windows:
java -cp ".;path_to_javafx_libs/*" RelogioConfig

- No Linux/Mac:
java -cp ".:path_to_javafx_libs/*" RelogioConfig

# Observações:
- Substitua path_to_javafx_libs pelo caminho das bibliotecas JavaFX no seu sistema.
- Tenha o Java 17+ instalado e configurado.
- Pode usar IDEs como IntelliJ, Eclipse ou NetBeans para facilitar o processo.
