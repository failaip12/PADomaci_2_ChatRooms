package rs.raf.pds.v4.z5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.UUID;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import javafx.application.Platform;
import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.ChatMessageLinkedHashSet;
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
	
    private LinkedHashSet<ChatMessage> chatHistory;
	
	public ChatClient(ChatClientGUI gui, String hostName, int portNumber, String userName) {
		this.client = new Client(DEFAULT_CLIENT_WRITE_BUFFER_SIZE, DEFAULT_CLIENT_READ_BUFFER_SIZE);
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.userName = userName;
		this.gui = gui;
		this.chatHistory = new LinkedHashSet<ChatMessage>();
		KryoUtil.registerKryoClasses(client.getKryo());
		registerListener();
	}
    
    public LinkedHashSet<ChatMessage> getChatHistory() {
        return chatHistory;
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
						gui.displayMessages();
					});
					return;
				}

				if (object instanceof ListUsers) {
					ListUsers listUsers = (ListUsers)object;
					showOnlineUsers(listUsers.getUsers());
					Platform.runLater(() -> {
						gui.displayMessages();
					});
					return;
				}
				
				if (object instanceof InfoMessage) {
					InfoMessage message = (InfoMessage)object;
					String text = message.getTxt();
					showMessage("Server:"+text);
					Platform.runLater(() -> {
						gui.displayMessages();
					});
					return;
				}
				
				if (object instanceof UpdatedChatMessage) {
					UpdatedChatMessage message = (UpdatedChatMessage)object;
					updateChatMessage(message);
					Platform.runLater(() -> {
						gui.displayMessages();
					});
					return;
				}
				
				if (object instanceof ChatMessageLinkedHashSet) {
					ChatMessageLinkedHashSet messageList = (ChatMessageLinkedHashSet)object;
					chatHistory = messageList.getMessageList();
					Platform.runLater(() -> {
						gui.displayMessages();
					});
					return;
				}
			}
			
			public void disconnected(Connection connection) {
				
			}
		});
	}
	
	private void updateChatMessage(UpdatedChatMessage chatMessage) {
		for(ChatMessage message:chatHistory) {
			if(message.getMessageId().equals(chatMessage.getMessageId())) {
				message.setTxt(chatMessage.getTxt());
				message.setEdited();
			}
			if(message.isReply() && message.getMessageRepliedTo().getMessageId().equals(chatMessage.getMessageId())) {
				message.getMessageRepliedTo().setTxt(chatMessage.getTxt());
			}
		}
		System.out.println(chatMessage.getUser()+":"+chatMessage.getTxt());
	}
	
	private void showChatMessage(ChatMessage chatMessage) {
		chatHistory.add(chatMessage);
		System.out.println(chatMessage.getUser()+":"+chatMessage.getTxt());
	}
	
	private void showMessage(String txt) {
		chatHistory.add(new ChatMessage(null, txt));
		System.out.println(txt);
	}
	private void showOnlineUsers(String[] users) {
		System.out.print("Server:");
		chatHistory.add(new ChatMessage(null, "Server:"));
		for (int i=0; i<users.length; i++) {
			String user = users[i];
			System.out.print(user);
			chatHistory.add(new ChatMessage(null, user));
			System.out.printf((i==users.length-1?"\n":", "));
			chatHistory.add(new ChatMessage(null, (i==users.length-1?"\n":", ")));
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
	                client.update(0); // Handle communication in a separate thread
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    });
	    thread.start();
	}
	
	public void sendMessage(String message) {
	    if (client.isConnected()) {
	        client.sendTCP(new ChatMessage(userName, message));
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
	
	public void editMessage(UUID idMessage, String message) {
	    if (client.isConnected()) {
	        client.sendTCP(new EditMessage(idMessage, userName, message));
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
