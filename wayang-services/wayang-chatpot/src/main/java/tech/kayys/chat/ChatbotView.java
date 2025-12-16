package tech.kayys.chat;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import tech.kayys.wayang.engine.LlamaConfig;
import tech.kayys.wayang.model.ChatMessage;

public class ChatbotView extends BorderPane {
    private static final Logger log = LoggerFactory.getLogger(ChatbotView.class);
    
    private final VBox chatContainer;
    private final ScrollPane scrollPane;
    private final TextArea inputArea;
    private final Button sendButton;
    private final ProgressIndicator progressIndicator;
    private final ChatbotController controller;
    private final List<ChatMessage> conversationHistory;
    
    public ChatbotView() {
        this.conversationHistory = new ArrayList<>();
        this.controller = new ChatbotController();
        
        // Header
        HBox header = createHeader();
        setTop(header);
        
        // Chat area
        chatContainer = new VBox(10);
        chatContainer.setPadding(new Insets(20));
        chatContainer.getStyleClass().add("chat-container");
        
        scrollPane = new ScrollPane(chatContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("chat-scroll");
        setCenter(scrollPane);
        
        // Input area
        VBox inputContainer = createInputArea();
        setBottom(inputContainer);
        
        // Initialize UI elements
        inputArea = (TextArea) inputContainer.lookup(".input-area");
        sendButton = (Button) inputContainer.lookup(".send-button");
        progressIndicator = (ProgressIndicator) inputContainer.lookup(".progress-indicator");
        
        // Setup event handlers
        setupEventHandlers();
        
        // Add welcome message
        addWelcomeMessage();
    }
    
    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setPadding(new Insets(15));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        
        Label titleLabel = new Label("🦙 Llama Chat Assistant");
        titleLabel.getStyleClass().add("header-title");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button settingsButton = new Button("⚙");
        settingsButton.getStyleClass().add("icon-button");
        settingsButton.setOnAction(e -> showSettingsDialog());
        
        Button clearButton = new Button("🗑");
        clearButton.getStyleClass().add("icon-button");
        clearButton.setOnAction(e -> clearChat());
        
        header.getChildren().addAll(titleLabel, spacer, settingsButton, clearButton);
        return header;
    }
    
    private VBox createInputArea() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.getStyleClass().add("input-container");
        
        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER);
        
        TextArea textArea = new TextArea();
        textArea.getStyleClass().add("input-area");
        textArea.setPromptText("Type your message here... (Shift+Enter for new line)");
        textArea.setPrefRowCount(3);
        textArea.setWrapText(true);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        
        VBox buttonBox = new VBox(5);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button sendBtn = new Button("Send");
        sendBtn.getStyleClass().addAll("send-button", "primary-button");
        sendBtn.setPrefWidth(80);
        
        ProgressIndicator progress = new ProgressIndicator();
        progress.getStyleClass().add("progress-indicator");
        progress.setMaxSize(30, 30);
        progress.setVisible(false);
        
        buttonBox.getChildren().addAll(sendBtn, progress);
        inputBox.getChildren().addAll(textArea, buttonBox);
        
        // Model info
        Label modelInfo = new Label("Model: Not loaded");
        modelInfo.getStyleClass().add("model-info");
        
        container.getChildren().addAll(inputBox, modelInfo);
        return container;
    }
    
    private void setupEventHandlers() {
        sendButton.setOnAction(e -> sendMessage());
        
        inputArea.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER") && !e.isShiftDown()) {
                e.consume();
                sendMessage();
            }
        });
        
        // Auto-scroll
        chatContainer.heightProperty().addListener((obs, oldVal, newVal) -> 
            scrollPane.setVvalue(1.0));
    }
    
    private void addWelcomeMessage() {
        VBox welcomeBox = new VBox(10);
        welcomeBox.getStyleClass().add("welcome-message");
        welcomeBox.setPadding(new Insets(20));
        welcomeBox.setAlignment(Pos.CENTER);
        
        Label title = new Label("Welcome to Llama Chat!");
        title.getStyleClass().add("welcome-title");
        
        Label subtitle = new Label("Configure your model settings to get started");
        subtitle.getStyleClass().add("welcome-subtitle");
        
        Button configButton = new Button("Configure Model");
        configButton.getStyleClass().add("primary-button");
        configButton.setOnAction(e -> showSettingsDialog());
        
        welcomeBox.getChildren().addAll(title, subtitle, configButton);
        chatContainer.getChildren().add(welcomeBox);
    }
    
    private void sendMessage() {
        String message = inputArea.getText().trim();
        if (message.isEmpty() || !controller.isInitialized()) {
            return;
        }
        
        // Add user message
        conversationHistory.add(new ChatMessage("user", message));
        addMessageBubble("user", message);
        inputArea.clear();
        
        // Show loading
        setInputEnabled(false);
        progressIndicator.setVisible(true);
        
        // Create assistant message bubble
        VBox assistantBubble = createMessageBubble("assistant", "");
        TextFlow assistantText = (TextFlow) assistantBubble.lookup(".message-text");
        
        // Generate response
        new Thread(() -> {
            try {
                StringBuilder response = new StringBuilder();
                
                controller.chat(conversationHistory, piece -> {
                    response.append(piece);
                    Platform.runLater(() -> {
                        Text text = new Text(response.toString());
                        assistantText.getChildren().setAll(text);
                    });
                });
                
                conversationHistory.add(new ChatMessage("assistant", response.toString()));
                
            } catch (Exception e) {
                log.error("Generation failed", e);
                Platform.runLater(() -> showError("Generation failed: " + e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    setInputEnabled(true);
                    progressIndicator.setVisible(false);
                });
            }
        }).start();
    }
    
    private void addMessageBubble(String role, String content) {
        VBox bubble = createMessageBubble(role, content);
        chatContainer.getChildren().add(bubble);
    }
    
    private VBox createMessageBubble(String role, String content) {
        VBox bubble = new VBox(5);
        bubble.getStyleClass().addAll("message-bubble", role + "-bubble");
        bubble.setPadding(new Insets(12));
        bubble.setMaxWidth(700);
        
        Label roleLabel = new Label(role.equals("user") ? "You" : "Assistant");
        roleLabel.getStyleClass().add("role-label");
        
        TextFlow textFlow = new TextFlow();
        textFlow.getStyleClass().add("message-text");
        Text text = new Text(content);
        textFlow.getChildren().add(text);
        
        bubble.getChildren().addAll(roleLabel, textFlow);
        
        HBox container = new HBox();
        if (role.equals("user")) {
            container.setAlignment(Pos.CENTER_RIGHT);
        } else {
            container.setAlignment(Pos.CENTER_LEFT);
        }
        container.getChildren().add(bubble);
        
        chatContainer.getChildren().add(container);
        return bubble;
    }
    
    private void showSettingsDialog() {
        Dialog<LlamaConfig> dialog = new Dialog<>();
        dialog.setTitle("Model Settings");
        dialog.setHeaderText("Configure Llama Model");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField libraryPath = new TextField("/usr/local/lib/libllama.so");
        TextField modelPath = new TextField();
        TextField contextSize = new TextField("4096");
        TextField threads = new TextField("8");
        TextField gpuLayers = new TextField("0");
        
        grid.add(new Label("Library Path:"), 0, 0);
        grid.add(libraryPath, 1, 0);
        grid.add(new Label("Model Path:"), 0, 1);
        grid.add(modelPath, 1, 1);
        grid.add(new Label("Context Size:"), 0, 2);
        grid.add(contextSize, 1, 2);
        grid.add(new Label("Threads:"), 0, 3);
        grid.add(threads, 1, 3);
        grid.add(new Label("GPU Layers:"), 0, 4);
        grid.add(gpuLayers, 1, 4);
        
        Button downloadButton = new Button("Download Model from HuggingFace");
        downloadButton.setOnAction(e -> showDownloadDialog());
        grid.add(downloadButton, 0, 5, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                try {
                    return LlamaConfig.builder()
                        .libraryPath(libraryPath.getText())
                        .modelPath(modelPath.getText())
                        .contextSize(Integer.parseInt(contextSize.getText()))
                        .threads(Integer.parseInt(threads.getText()))
                        .gpuLayers(Integer.parseInt(gpuLayers.getText()))
                        .build();
                } catch (Exception ex) {
                    showError("Invalid configuration: " + ex.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(config -> {
            initializeModel(config);
        });
    }
    
    private void showDownloadDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Download Model");
        dialog.setHeaderText("Download from HuggingFace");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField repoId = new TextField("TheBloke/Llama-2-7B-Chat-GGUF");
        TextField filename = new TextField("llama-2-7b-chat.Q4_K_M.gguf");
        TextField downloadDir = new TextField("./models");
        
        grid.add(new Label("Repository:"), 0, 0);
        grid.add(repoId, 1, 0);
        grid.add(new Label("Filename:"), 0, 1);
        grid.add(filename, 1, 1);
        grid.add(new Label("Download Dir:"), 0, 2);
        grid.add(downloadDir, 1, 2);
        
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        Label progressLabel = new Label("Ready to download");
        
        grid.add(progressBar, 0, 3, 2, 1);
        grid.add(progressLabel, 0, 4, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Download");
        okButton.setOnAction(e -> {
            controller.downloadModel(repoId.getText(), filename.getText(), 
                downloadDir.getText(), progress -> {
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress.percentage());
                        progressLabel.setText(String.format("%.2f%% (%d MB / %d MB)",
                            progress.percentage() * 100,
                            progress.downloaded() / 1024 / 1024,
                            progress.total() / 1024 / 1024));
                    });
                });
        });
        
        dialog.show();
    }
    
    private void initializeModel(LlamaConfig config) {
        progressIndicator.setVisible(true);
        
        new Thread(() -> {
            try {
                controller.initialize(config);
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    clearChat();
                    Label modelInfo = (Label) lookup(".model-info");
                    modelInfo.setText("Model: " + controller.getModelInfo());
                    showInfo("Model loaded successfully!");
                });
            } catch (Exception e) {
                log.error("Failed to initialize model", e);
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    showError("Failed to load model: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void clearChat() {
        conversationHistory.clear();
        chatContainer.getChildren().clear();
        addWelcomeMessage();
    }
    
    private void setInputEnabled(boolean enabled) {
        inputArea.setDisable(!enabled);
        sendButton.setDisable(!enabled);
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
