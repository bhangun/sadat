package tech.kayys.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatbotApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(ChatbotApp.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            ChatbotView view = new ChatbotView();
            
            Scene scene = new Scene(view, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            
            primaryStage.setTitle("Llama Chat Assistant");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            log.info("JavaFX application started");
            
        } catch (Exception e) {
            log.error("Failed to start application", e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void stop() {
        log.info("Application stopping...");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
