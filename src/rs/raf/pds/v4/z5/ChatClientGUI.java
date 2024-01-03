package rs.raf.pds.v4.z5;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import rs.raf.pds.v4.z5.messages.ChatMessage;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Optional;

public class ChatClientGUI extends Application {
    private ChatClient chatClient;

    private Stage primaryStage;
    private ListView<ChatMessage> chatListView;
    private TextField inputField;
    private TextField usernameField;
    private TextField hostnameField;
    private TextField portField;
    private Scene chatScene;
    public String username;
    private LinkedHashSet<ChatMessage> messages = new LinkedHashSet<ChatMessage>();

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
        username = usernameField.getText().trim();
        String hostname = hostnameField.getText().trim();
        String portStr = portField.getText().trim();

        if (!username.isEmpty() && !hostname.isEmpty() && !portStr.isEmpty()) {
            int port = Integer.parseInt(portStr);
            chatClient = new ChatClient(this, hostname, port, username);
            try {
                chatClient.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            switchToChatWindow();
            // Start a separate thread to listen for incoming messages and update the GUI

        }
    }

    private void switchToChatWindow() {

    	BorderPane borderPane = new BorderPane();
        chatListView = new ListView<>();
        
        chatListView.setCellFactory(param -> new ListCell<ChatMessage>() {
            private final ContextMenu contextMenu = new ContextMenu();

            {
                MenuItem editItem = new MenuItem("Edit");
                editItem.setOnAction(e -> {
                    ChatMessage selectedMessage = getItem();
                    if (selectedMessage != null && username.equals(selectedMessage.getSender())) {
                        editMessage(selectedMessage);
                    }
                });

                MenuItem replyItem = new MenuItem("Reply");
                replyItem.setOnAction(e -> {
                    ChatMessage selectedMessage = getItem();
                    if (selectedMessage != null && selectedMessage.getSender() != null) {
                        replyToMessage(selectedMessage);
                    }
                });

                contextMenu.getItems().addAll(editItem, replyItem);
            }

            @Override
            protected void updateItem(ChatMessage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                	setTextFill(Color.BLACK);
                    String text = item.toString();
                    if (item.isEdited()) {
                        text += " (Ed)";
                    }
                    if(item.isPrivateMessage()) {
                    	if(item.getSender().equals(username)) {
                    		text = "(PRIVATE) TO:" + text;
                    	}
                    	else if(item.getReciever().equals(username)) {
                    		text = "(PRIVATE) FROM:" + text;
                    	}
                    }
                    if (item.isReply()) {
                    	setTextFill(Color.GREEN);
                        text = "Reply to	" + item.getMessageRepliedTo().getSender() + ": " + item.getMessageRepliedTo().getTxt() + "\n" + text;
                        setOnMouseClicked(event -> {
                            if (event.getButton() == MouseButton.PRIMARY) {
                                jumpToOriginalMessage(item.getMessageRepliedTo());
                            }
                        });
                    } else {
                        setOnMouseClicked(null);
                    }
                    setText(text);
                    setContextMenu(contextMenu);
                }
            }
        });

        chatListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume(); // Consume the event to prevent it from interfering with cell selection
            }
        });

        borderPane.setCenter(chatListView);

        inputField = new TextField();
        inputField.setOnAction(e -> sendMessage());
        VBox vBox = new VBox(10);
        vBox.getChildren().addAll(borderPane, inputField);

        chatScene = new Scene(vBox, 400, 300);
        primaryStage.setScene(chatScene);
    }
    
    private void jumpToOriginalMessage(ChatMessage originalMessage) {//So close yet so far.
    	ObservableList<ChatMessage> items = chatListView.getItems();
    	int index = -1;
    	for(ChatMessage item1:items) {
    		if(item1.equals(originalMessage)) {
    			index = chatListView.getItems().indexOf(item1);
    		}
    	}
        if (index < 0) {
        	chatClient.jumpToMessageFetch(originalMessage);
        }
        chatListView.scrollTo(index);
    }
    
    private void editMessage(ChatMessage selectedMessage) {
        TextInputDialog dialog = new TextInputDialog(selectedMessage.getTxt());
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Edit your message:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(editedText -> {
        	chatClient.editMessage(selectedMessage, editedText);
            selectedMessage.setTxt(editedText);
            displayMessages();
        });
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            chatClient.sendMessage(message);
            inputField.clear();
        }
    }

    private void replyToMessage(ChatMessage selectedMessage) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reply to Message");
        dialog.setHeaderText("Reply to " + selectedMessage.getSender() + "'s message:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(replyText -> {
            ChatMessage message = new ChatMessage(username, replyText);
            message.setReply();
            message.setMessageRepliedTo(selectedMessage);
        	if(selectedMessage.isPrivateMessage()) {
        		message.setPrivateMessage();
        	}
            chatClient.sendReply(message);
        });
    }

    public void displayMessages() {
        messages = chatClient.getChatHistory();
        chatListView.getItems().setAll(messages);
        chatListView.scrollTo(chatListView.getItems().size() - 1);
    }
}
