package rs.raf.pds.v4.z5;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import rs.raf.pds.v4.z5.messages.ChatMessage;

public class ChatClientGUI extends Application {
    private ChatClient chatClient;
    
    private Stage primaryStage;
    private TextArea chatArea;
    private TextField inputField;
    private TextField usernameField;
    private TextField hostnameField;
    private TextField portField;
    private Scene chatScene;
    
    private TextField editField;
    private Button editButton;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Chat Client");

        usernameField = new TextField();
        usernameField.setPromptText("Enter username");

        hostnameField = new TextField();
        hostnameField.setPromptText("Enter hostname");
        hostnameField.setText("localhost");
        
        portField = new TextField();
        portField.setPromptText("Enter port");
        portField.setText("4555");
        
        Button joinButton = new Button("Join");
        joinButton.setOnAction(e -> joinChat());

        VBox initialLayout = new VBox(
            new Label("Username:"),
            usernameField,
            new Label("Hostname:"),
            hostnameField,
            new Label("Port:"),
            portField,
            joinButton
        );

        initialLayout.setSpacing(10);
        initialLayout.setPadding(new Insets(10));

        Scene initialScene = new Scene(initialLayout, 600, 300);
        primaryStage.setScene(initialScene);

        primaryStage.setOnCloseRequest(e -> {
            if (chatClient != null) {
                chatClient.stop();
            }
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    private void joinChat() {
        String username = usernameField.getText().trim();
        String hostname = hostnameField.getText().trim();
        String portStr = portField.getText().trim();

        if (!username.isEmpty() && !hostname.isEmpty() && !portStr.isEmpty()) {
            int port = Integer.parseInt(portStr);
            chatClient = new ChatClient(hostname, port, username);
            try {
                chatClient.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            switchToChatWindow();
            // Start a separate thread to listen for incoming messages and update the GUI
            new Thread(() -> {
                while (chatClient.isRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    if(primaryStage.getScene() == chatScene) {
                    	Platform.runLater(() -> updateChatArea());
                    }
                }
            }).start();
        }
    }
    
    private void switchToChatWindow() {
        chatArea = new TextArea();
        chatArea.setEditable(false);
        inputField = new TextField();
        inputField.setOnAction(e -> sendMessage(inputField, chatArea));

        HBox inputBox = new HBox(inputField);
        editField = new TextField();
        editField.setPromptText("Edit your message");

        editButton = new Button("Edit");
        editButton.setOnAction(e -> editMessage(editField, chatArea));

        HBox editBox = new HBox(editField, editButton);

        VBox chatLayout = new VBox(chatArea, inputBox, editBox); // Add editBox to include editing UI
        chatLayout.setSpacing(10);
        chatLayout.setPadding(new Insets(10));

        chatScene = new Scene(chatLayout, 400, 300);
        primaryStage.setScene(chatScene);
    }
    

    private void editMessage(TextField editField, TextArea chatArea) {
        String editedMessage = editField.getText().trim();
        if (!editedMessage.isEmpty()) {
            // Replace the selected message with the edited message
            int selectedIndex = chatArea.getSelection().getStart();
            if (selectedIndex >= 0 && selectedIndex < chatClient.getChatHistory().size()) {
                ChatMessage selectedMessage = chatClient.getChatHistory().get(selectedIndex);
                selectedMessage.setTxt(editedMessage);
                updateChatArea();
                // You might want to notify the server about the edit action here
            }
        }
        editField.clear();
    }
    
    private void sendMessage(TextField inputField, TextArea chatArea) {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            chatClient.sendMessage(message);
//            chatArea.appendText("You: " + message + "\n");
            inputField.clear();
        }
    }

    private void updateChatArea() {
        String receivedMessage = chatClient.receiveMessage();
        if (receivedMessage != null && !receivedMessage.isEmpty()) {
            chatArea.appendText(receivedMessage + "\n");
        }
    }
}
