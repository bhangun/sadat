package tech.kayys.chat;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import tech.kayys.chat.MCPConfigDialog.MCPServerConfig;

public class MCPConfigDialog {
    
    public static Optional<MCPServerConfig> showConfigDialog() {
        Dialog<MCPServerConfig> dialog = new Dialog<>();
        dialog.setTitle("Configure MCP Server");
        dialog.setHeaderText("Add a new MCP server");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Server name");
        
        TextField commandField = new TextField();
        commandField.setPromptText("npx");
        
        TextField argsField = new TextField();
        argsField.setPromptText("-y @modelcontextprotocol/server-filesystem /path");
        
        TextArea envArea = new TextArea();
        envArea.setPromptText("KEY1=value1\nKEY2=value2");
        envArea.setPrefRowCount(3);
        
        TextField descField = new TextField();
        descField.setPromptText("Description");
        
        CheckBox enabledCheck = new CheckBox("Enabled");
        enabledCheck.setSelected(true);
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Command:"), 0, 1);
        grid.add(commandField, 1, 1);
        grid.add(new Label("Arguments:"), 0, 2);
        grid.add(argsField, 1, 2);
        grid.add(new Label("Environment:"), 0, 3);
        grid.add(envArea, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descField, 1, 4);
        grid.add(enabledCheck, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new MCPServerConfig(
                    nameField.getText(),
                    commandField.getText(),
                    argsField.getText().split("\\s+"),
                    envArea.getText(),
                    descField.getText(),
                    enabledCheck.isSelected()
                );
            }
            return null;
        });
        
        return dialog.showAndWait();
    }
    
    public record MCPServerConfig(
        String name,
        String command,
        String[] args,
        String env,
        String description,
        boolean enabled
    ) {}
}
