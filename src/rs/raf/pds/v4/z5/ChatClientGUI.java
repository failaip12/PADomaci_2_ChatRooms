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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

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
    public ChatRoom currentChatRoom;
    private ListView<ChatRoom> chatRoomList = new ListView<>();
    public static void main(String[] args) {
        launch(args);
    }

    public ChatRoom getCurrentChatRoom() {
		return currentChatRoom;
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
                
                MenuItem deleteItem = new MenuItem("Delete");
                deleteItem.setOnAction(e -> {
                    ChatMessage selectedMessage = getItem();
                    if (selectedMessage != null && username.equals(selectedMessage.getSender())) {
                        deleteMessage(selectedMessage);
                    }
                });
                
                contextMenu.getItems().addAll(editItem, replyItem, deleteItem);
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
                    		text = "(PRIVATE) TO " + item.getReciever() + "\n" + item.getTxt();
                            if (item.isEdited()) {
                                text += " (Ed)";
                            }
                    	}
                    	else if(item.getReciever().equals(username)) {
                    		text = "(PRIVATE) FROM " + item.getSender() + "\n" + item.getTxt();
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
                event.consume();
            }
        });

        borderPane.setCenter(chatListView);
        
        inputField = new TextField();
        inputField.setOnAction(e -> sendMessage());
        VBox vBox = new VBox(10);
        vBox.getChildren().addAll(borderPane, inputField);
        addCreateChatRoomButton(borderPane);
        chatRoomList.getItems().addAll(chatClient.chatRoomsNameMap.values());
        chatRoomList.setCellFactory(param -> new ListCell<ChatRoom>() {
            @Override
            protected void updateItem(ChatRoom item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if(item.isPrivate_chat()) {
                    Set<String> userList = item.getUserList();
                    if (userList.size() == 2) {
                        Iterator<String> iterator = userList.iterator();
                        String user1 = iterator.next();
                        String user2 = iterator.next();
                        
                        String otherUser = user1.equals(chatClient.userName) ? user2 : user1;
                    	setText("Private Chat with : " + otherUser);
                    }
                    else {
                    	setText("UNREACHABLE");
                    }
                }
                else {
                    // Customize how the room name is displayed here
                    setText("Room: " + item.getRoomName());
                }
            }
        });

        chatRoomList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                ChatRoom selectedChatRoom = chatRoomList.getSelectionModel().getSelectedItem();
                if (selectedChatRoom != null) {
                	switchChatRoom(selectedChatRoom);
                }
            }
        });
        
        
        chatRoomList.setPrefWidth(120); 
        borderPane.setRight(chatRoomList);
        
        currentChatRoom = new ChatRoom(null, null);
        messages = currentChatRoom.getMessageHistory();
        chatListView.getItems().setAll(messages);
        chatScene = new Scene(vBox, 800, 600);
        primaryStage.setScene(chatScene);
        updateChatRoomList();
    }
    
    private String promptForNewChatRoom() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create New Chat Room");
        dialog.setHeaderText("Enter the name for the new chat room:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void addCreateChatRoomButton(BorderPane borderPane) {
        Button createChatRoomButton = new Button("Create Chat Room");
        createChatRoomButton.setOnAction(e -> createNewChatRoom());
        borderPane.setBottom(createChatRoomButton);
    }
    
    private void createNewChatRoom() {
        String newChatRoomName = promptForNewChatRoom();
        if (newChatRoomName != null && !newChatRoomName.trim().isEmpty()) {
            createChatRoom(newChatRoomName);
        }
    }
    
    public void updateChatRoomList() {
        Scene scene = primaryStage.getScene();
        VBox vBox = (VBox) scene.getRoot();

        BorderPane borderPane = (BorderPane) vBox.getChildren().get(0);
        ListView<ChatRoom> chatRoomList = (ListView<ChatRoom>) borderPane.getRight(); //This should be safe anyway

        chatRoomList.getItems().setAll(chatClient.chatRoomsNameMap.values());
    }
    
    public void addChatRoom(String chatRoomName, Boolean private_room) {
        ChatRoom room = new ChatRoom(username, chatRoomName);
        if(private_room) {
        	room.isPrivate_chat();
        }
    }
    
    private void createChatRoom(String chatRoomName) {
        chatClient.sendMessage("/create " + chatRoomName, currentChatRoom.getRoomName());
        addChatRoom(chatRoomName, false);
    }
    
    private void switchChatRoom(ChatRoom chatRoom) {
        if(chatRoom.isPrivate_chat()) {
            Set<String> userList = chatRoom.getUserList();
            if (userList.size() == 2) {
                Iterator<String> iterator = userList.iterator();
                String user1 = iterator.next();
                String user2 = iterator.next();
                
                String otherUser = user1.equals(chatClient.userName) ? user2 : user1;
            	chatClient.sendMessage("/join_private_chat " + otherUser, currentChatRoom.getRoomName());
            } else {
                System.err.println("UNREACHABLE");
                return;
            }
        }
        else {
        	chatClient.sendMessage("/join " + chatRoom.getRoomName(), currentChatRoom.getRoomName());
        }
        displayMessages(chatRoom);
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
        	chatClient.sendEditMessage(selectedMessage, editedText);
            selectedMessage.setTxt(editedText);
            displayMessages(getCurrentChatRoom());
        });
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            chatClient.sendMessage(message, currentChatRoom.getRoomName());
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
        	if(selectedMessage.isPrivateMessage()) { //TODO: This should probably be done on server side.
        		message.setPrivateMessage();
        	}
        	message.setRoomName(selectedMessage.getRoomName());
            chatClient.sendReply(message);
        });
    }
    
    private void deleteMessage(ChatMessage selectedMessage) {
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Delete Message");
        confirmationDialog.setHeaderText("Are you sure you want to delete this message?");
        confirmationDialog.setContentText(selectedMessage.getTxt());

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
        	chatClient.sendDeleteMessage(selectedMessage, username);
        }
    }
    
    public void displayMessages(ChatRoom room) {
    	if(room!=null) {
	        messages = chatClient.chatRoomsHistory.get(room);
	        if (messages != null && chatListView.getItems() != null) {
	            chatListView.getItems().setAll(messages);
	            chatListView.scrollTo(chatListView.getItems().size() - 1);
	        } else if(messages == null) {
	        	chatListView.getItems().clear();
	        }
	        else if(chatListView.getItems() == null) {
	        	System.out.println("Error: chatListView.getItems() is null");
	        }
    	}
    	else {
    		System.out.println("room is null");
    	}
    }
}
