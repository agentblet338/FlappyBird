package com.flappy;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlappyBirdController {

    private VBox gameOverBox;
    private Label lblResult;

    private static class PipePair {
        final Rectangle top;
        final Rectangle bottom;
        boolean passed = false;

        PipePair(double x, double topHeight, double bottomHeight, double gap) {
            this.top = createPipe(x, 0, topHeight);
            this.bottom = createPipe(x, 470 - bottomHeight, bottomHeight);
        }

        private static Rectangle createPipe(double x, double y, double height) {
            Rectangle pipe = new Rectangle(x, y, 60, height);
            pipe.setFill(Color.web("#15803d"));
            pipe.setStroke(Color.web("#020617"));
            pipe.setStrokeWidth(2.5);
            return pipe;
        }

        void moveX(double speed) {
            top.setX(top.getX() - speed);
            bottom.setX(bottom.getX() - speed);
        }

        double getX() { return top.getX(); }

        boolean intersects(Circle bird) {
            return top.getBoundsInParent().intersects(bird.getBoundsInParent()) ||
                    bottom.getBoundsInParent().intersects(bird.getBoundsInParent());
        }
    }

    @FXML private VBox menuPane, settingsPane;
    @FXML private Pane gamePane;
    @FXML private CheckBox chkSound, chkMusic;
    @FXML private ComboBox<String> listDiff, listBg, listBird;

    private Circle birdCircle;
    private Label scoreLabel;
    private final List<PipePair> pipePairs = new ArrayList<>();
    private final Random random = new Random();

    // Складність:
    // изи
    private double easyGap = 150.0;
    private double easySpeed = 2.2;
    private int easyInterval = 110;

    // норм
    private double mediumGap = 120.0;
    private double mediumSpeed = 2.8;
    private int mediumInterval = 90;

    // хард
    private double hardGap = 95.0;
    private double hardSpeed = 3.6;
    private int hardInterval = 75;

    // зараз
    private double pipeGap = mediumGap;
    private double pipeSpeed = mediumSpeed;
    private int pipeSpawnInterval = mediumInterval;
    //

    // дефлот налаштування
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private String difficulty = "Середній";
    private String bgTheme = "День";
    private String birdSkin = "Класична";

    // фіз час і тд
    private final double gravity = 0.4;
    private final double jumpStrength = -7.0;
    private AnimationTimer gameTimer;
    private double birdVelocityY = 0;
    private int score = 0;
    private boolean isGameOver = false, isGameRunning = false;
    private int frameCount = 0;

    // списки та установка дефолт налаштування
    @FXML
    public void initialize() {
        listDiff.getItems().addAll("Легкий", "Середній", "Важкий");
        listDiff.setValue(difficulty);
        listBg.getItems().addAll("День", "Ніч", "Ліс", "Ретро");
        listBg.setValue(bgTheme);
        listBird.getItems().addAll("Класична", "Блискавка", "Полум'я", "Кіборг");
        listBird.setValue(birdSkin);

        buildGameOverBox();
    }
    // кнопки для збереження налаштувань та перемикання між сцен
    @FXML private void onStartGameClick() { switchScreen(false, false, true); initGame(); }
    @FXML private void onOpenSettingsClick() { switchScreen(false, true, false); }
    @FXML private void onSaveSettingsClick() {
        soundEnabled = chkSound.isSelected();
        musicEnabled = chkMusic.isSelected();
        bgTheme = listBg.getValue();
        birdSkin = listBird.getValue();
        difficulty = listDiff.getValue();

        switch (difficulty) {
            case "Легкий" -> {
                pipeGap = easyGap;
                pipeSpeed = easySpeed;
                pipeSpawnInterval = easyInterval;
            }
            case "Важкий" -> {
                pipeGap = hardGap;
                pipeSpeed = hardSpeed;
                pipeSpawnInterval = hardInterval;
            }
            default -> {
                pipeGap = mediumGap;
                pipeSpeed = mediumSpeed;
                pipeSpawnInterval = mediumInterval;
            }
        }
        switchScreen(true, false, false);
    }

    private void switchScreen(boolean menu, boolean settings, boolean game) {
        menuPane.setVisible(menu);
        settingsPane.setVisible(settings);
        gamePane.setVisible(game);
    }

    // підготовка та рахунок
    private void initGame() {
        isGameOver = false; isGameRunning = false; score = 0; frameCount = 0; birdVelocityY = 0;
        pipePairs.clear();
        gamePane.getChildren().clear();

        applyBackgroundTheme();

        birdCircle = new Circle(100, 200, 13, Color.web(getBirdColorHex()));
        birdCircle.setStroke(Color.web("#020617"));
        birdCircle.setStrokeWidth(2.0);

        scoreLabel = new Label("Рахунок: 0");
        scoreLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setLayoutX(15); scoreLabel.setLayoutY(15);

        gamePane.getChildren().addAll(birdCircle, scoreLabel);
        gamePane.getScene().setOnKeyPressed(e -> { if (e.getCode() == KeyCode.SPACE) handleAction(); });
        gamePane.getScene().setOnMouseClicked(e -> handleAction());
    }

    private void handleAction() {
        if (isGameOver) {
            switchScreen(true, false, false);
            if (gameTimer != null) gameTimer.stop();
            return;
        }
        if (!isGameRunning) {
            isGameRunning = true;
            startGameLoop();
        }
        birdVelocityY = jumpStrength;
        playSound("jump!");
    }

    private void startGameLoop() {
        // не дуже зрозуміло було як працюють анимації
        if (gameTimer != null) gameTimer.stop();
        gameTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isGameOver) { stop(); return; }

                // Физика птицы
                birdVelocityY += gravity;
                birdCircle.setCenterY(birdCircle.getCenterY() + birdVelocityY);

                if (++frameCount % pipeSpawnInterval == 0) spawnPipes();

                updatePipes();
                checkCollisions();
            }
        };
        gameTimer.start();
    }

    // генерація труб та створення пар для труб зверху та снизу
    private void spawnPipes() {
        double height = 470;
        double minHeight = 60;
        double maxHeight = height - pipeGap - minHeight;
        double topHeight = minHeight + random.nextDouble() * (maxHeight - minHeight);
        double bottomHeight = height - topHeight - pipeGap;

        PipePair pair = new PipePair(400, topHeight, bottomHeight, pipeGap);
        pipePairs.add(pair);
        gamePane.getChildren().addAll(pair.top, pair.bottom);
    }

    // механіка рахунку
    private void updatePipes() {
        pipePairs.removeIf(pair -> {
            pair.moveX(pipeSpeed);

            if (!pair.passed && pair.getX() + 60 < birdCircle.getCenterX()) {
                pair.passed = true;
                score++;
                scoreLabel.setText("Рахунок: " + score);
                // ТЕСТ
                playSound("score point!");
            }

            if (pair.getX() < -80) {
                gamePane.getChildren().removeAll(pair.top, pair.bottom);
                return true;
            }
            return false;
        });
    }

    private void checkCollisions() {
        if (birdCircle.getCenterY() < 0 || birdCircle.getCenterY() > 457) { // 550 - 80 - 13
            gameOver();
            return;
        }
        for (PipePair pair : pipePairs) {
            if (pair.intersects(birdCircle)) {
                gameOver();
                return;
            }
        }
    }

    private void gameOver() {
        isGameOver = true;
        // ТЕСТ
        playSound("crash!");
        lblResult.setText("Ваш результат: " + score);
        if (!gamePane.getChildren().contains(gameOverBox)) {
            gamePane.getChildren().add(gameOverBox);
        }
    }

    private void buildGameOverBox() {
        gameOverBox = new VBox(15);
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setPadding(new Insets(20));
        gameOverBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.9); -fx-background-radius: 12px; -fx-border-color: #f43f5e; -fx-border-width: 2px; -fx-border-radius: 12px;");
        gameOverBox.setLayoutX(50); gameOverBox.setLayoutY(150); gameOverBox.setPrefWidth(300);

        Label lblOver = new Label("Гру завершено!");
        lblOver.setTextFill(Color.web("#f43f5e"));
        lblOver.setFont(Font.font("System", FontWeight.BOLD, 22));

        lblResult = new Label();
        lblResult.setTextFill(Color.WHITE);
        lblResult.setFont(Font.font("System", FontWeight.SEMI_BOLD, 16));

        Button btnRestart = new Button("Грати знову");
        btnRestart.getStyleClass().add("btn-primary");
        btnRestart.setOnAction(e -> initGame());

        Button btnMenu = new Button("В меню");
        btnMenu.getStyleClass().add("btn-secondary");
        btnMenu.setOnAction(e -> switchScreen(true, false, false));

        gameOverBox.getChildren().addAll(lblOver, lblResult, btnRestart, btnMenu);
    }

    private void applyBackgroundTheme() {
        String color = switch (bgTheme) {
            case "Ніч" -> "#0f172a";
            case "Ліс" -> "#14532d";
            case "Ретро" -> "#18181b";
            default -> "#38bdf8";
        };
        gamePane.setStyle("-fx-background-color: " + color + ";");
    }

    private String getBirdColorHex() {
        return switch (birdSkin) {
            case "Блискавка" -> "#22d3ee";
            case "Полум'я" -> "#f43f5e";
            case "Кіборг" -> "#a855f7";
            default -> "#facc15";
        };
    }
    // ТЕСТ
    private void playSound(String soundMessage) {
        if (soundEnabled) System.out.println("Sound: " + soundMessage);
    }
}