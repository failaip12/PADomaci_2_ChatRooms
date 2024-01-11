package rs.raf.pds.v4.z5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import javafx.application.Platform;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.ChatMessageLinkedHashSet;
import rs.raf.pds.v4.z5.messages.DeleteMessage;
import rs.raf.pds.v4.z5.messages.EditMessage;
import rs.raf.pds.v4.z5.messages.FetchMessages;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.UpdatedChatMessage;
import rs.raf.pds.v4.z5.messages.WhoRequest;

public class ChatClient implements Runnable{

	public static int DEFAULT_CLIENT_READ_BUFFER_SIZE = 1000000;
	public static int DEFAULT_CLIENT_WRITE_BUFFER_SIZE = 1000000;
	
	private volatile Thread thread = null;
	
	volatile boolean running = false;
	private ChatClientGUI gui;
	final Client client;
	final String hostName;
	final int portNumber;
	final String userName;
	
	public ConcurrentMap<String, ChatRoom> chatRoomsNameMap = new ConcurrentHashMap<>();
	
	public ChatClient(ChatClientGUI gui, String hostName, int portNumber, String userName) {
		this.client = new Client(DEFAULT_CLIENT_WRITE_BUFFER_SIZE, DEFAULT_CLIENT_READ_BUFFER_SIZE);
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.userName = userName;
		this.gui = gui;
		KryoUtil.registerKryoClasses(client.getKryo());
		registerListener();
	}
    
	private void registerListener() {
		client.addListener(new Listener() {
			public void connected (Connection connection) {
				Login loginMessage = new Login(userName);
				client.sendTCP(loginMessage);
			}
			
			public void received (Connection connection, Object object) {
				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage)object;
					showChatMessage(chatMessage);
					Platform.runLater(() -> {
						gui.displayMessages(gui.getCurrentChatRoom());
					});
					return;
				}

				if (object instanceof ListUsers) {
					ListUsers listUsers = (ListUsers)object;
					showOnlineUsers(listUsers.getUsers());
					Platform.runLater(() -> {
						gui.displayMessages(gui.getCurrentChatRoom());
					});
					return;
				}
				
				if (object instanceof ChatRoom) {
					ChatRoom room = (ChatRoom)object;
					chatRoomsNameMap.put(room.getRoomName(), room);
					Platform.runLater(() -> {
						gui.updateChatRoomList();
						gui.currentChatRoom = room;
						gui.displayMessages(gui.getCurrentChatRoom());
					});
					return;
				}
				
				if (object instanceof InfoMessage) {
					InfoMessage message = (InfoMessage)object;
					String text = message.getTxt();
					showMessage("(SERVER) :" + text);
					Platform.runLater(() -> {
						gui.displayMessages(gui.getCurrentChatRoom());
					});
					return;
				}
				
				if (object instanceof UpdatedChatMessage) {
					UpdatedChatMessage message = (UpdatedChatMessage)object;
					updateChatMessage(message);
					Platform.runLater(() -> {
						gui.displayMessages(gui.getCurrentChatRoom());
					});
					return;
				}
				
				if (object instanceof DeleteMessage) {
					DeleteMessage message = (DeleteMessage)object;
					deleteMessage(message);
					Platform.runLater(() -> {
						gui.displayMessages(gui.getCurrentChatRoom());
					});
					return;
				}
				
				if (object instanceof ChatMessageLinkedHashSet) {
					ChatMessageLinkedHashSet messageList = (ChatMessageLinkedHashSet)object;
					if(messageList.getMessageList().size() > 0) {
						ChatMessage message = messageList.getMessageList().stream().findAny().get();
						ChatRoom room = chatRoomsNameMap.get(message.getRoomName());
						room.setMessageHistory(messageList.getMessageList());
						Platform.runLater(() -> {
							gui.updateChatRoomList();
							gui.currentChatRoom = room;
							gui.displayMessages(gui.getCurrentChatRoom());
						});
						return;
					}
				}
			}
			
			public void disconnected(Connection connection) {
				
			}
		});
	}
	
	private void updateChatMessage(UpdatedChatMessage chatMessage) {
		ChatRoom room = chatRoomsNameMap.get(chatMessage.getRoomName());
		room.editMessage(chatMessage);
	}
	
	
	private void deleteMessage(DeleteMessage chatMessage) {
		ChatMessage message = chatMessage.getMessage();
		ChatRoom room = chatRoomsNameMap.get(message.getRoomName());
		room.deleteMessage(message);
	}
	
    private void showChatMessage(ChatMessage chatMessage) {
        String chatRoomName = chatMessage.getRoomName();
        ChatRoom room = chatRoomsNameMap.get(chatRoomName);
        room.addMessageToHistory(chatMessage);
    }
	
    private void showMessage(String txt) {
        ChatRoom room = gui.getCurrentChatRoom();
        room.addMessageToHistory(new ChatMessage(null, txt));
    }
    
	private void showOnlineUsers(String[] users) {
        ChatRoom room = gui.getCurrentChatRoom();
        room.addMessageToHistory(new ChatMessage(null, room.getRoomName() + ":"));
        for (int i = 0; i < users.length; i++) {
            String user = users[i];
            room.addMessageToHistory(new ChatMessage(null, user));
            room.addMessageToHistory(new ChatMessage(null, (i == users.length - 1 ? "\n" : ", ")));
        }
	}
	public void start() throws IOException {
		client.start();
		connect();
		
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}
	public void stop() {
		Thread stopThread = thread;
		thread = null;
		running = false;
		if (stopThread != null)
			stopThread.interrupt();
	}
	
	public void connect() throws IOException {
	    Thread thread = new Thread(() -> {
	        try {
	            client.connect(1000, hostName, portNumber);
	            while (running) {
	                client.update(0);
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    });
	    thread.start();
	}
	
	public void sendMessage(String text, String currentChatRoom) {
	    if (client.isConnected()) {
	    	ChatMessage message = new ChatMessage(userName, text);
	    	message.setRoomName(currentChatRoom);
	        client.sendTCP(message);
	    }
	}
	
	public void sendReply(ChatMessage message) {
	    if (client.isConnected()) {
	        client.sendTCP(message);
	    }
	}
	
	public void jumpToMessageFetch (ChatMessage message) {
		FetchMessages fMessage = new FetchMessages(message, userName);
	    if (client.isConnected()) {
	        client.sendTCP(fMessage);
	    }
	}
	
	public void sendEditMessage(ChatMessage message, String editedmessage) {
	    if (client.isConnected()) {
	        client.sendTCP(new EditMessage(message, userName, editedmessage));
	    }
	}
	
	public void sendDeleteMessage(ChatMessage message, String editedmessage) {
	    if (client.isConnected()) {
	        client.sendTCP(new DeleteMessage(message, userName));
	    }
	}
	
	public boolean isRunning() {
	    return running;
	}
	
	public void run() {
		
		try (
				BufferedReader stdIn = new BufferedReader(
	                    new InputStreamReader(System.in))	// Za čitanje sa standardnog ulaza - tastature!
	        ) {
					            
				String userInput;
				running = true;
				
	            while (running) {
	            	userInput = stdIn.readLine();
	            	if (userInput == null || "BYE".equalsIgnoreCase(userInput)) // userInput - tekst koji je unet sa tastature!
	            	{
	            		running = false;
	            	}
	            	else if ("WHO".equalsIgnoreCase(userInput)){
	            		client.sendTCP(new WhoRequest());
	            	}							
	            	else {
	            		ChatMessage message = new ChatMessage(userName, userInput);
	            		client.sendTCP(message);
	            	}
	            	
	            	if (!client.isConnected() && running)
	            		connect();
	            	
	           }
	            
	    } catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			running = false;
			System.out.println("CLIENT SE DISCONNECTUJE");
			client.close();;
		}
	}
	public static void main(String[] args) {
		if (args.length != 3) {
		
            System.err.println(
                "Usage: java -jar chatClient.jar <host name> <port number> <username>");
            System.out.println("Recommended port number is 54555");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String userName = args[2];
        
        try{
        	ChatClient chatClient = new ChatClient(null, hostName, portNumber, userName);
        	chatClient.start();
        }catch(IOException e) {
        	e.printStackTrace();
        	System.err.println("Error:"+e.getMessage());
        	System.exit(-1);
        }
	}
}
