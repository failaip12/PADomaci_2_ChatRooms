package rs.raf.pds.v4.z5;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.WhoRequest;


public class ChatServer implements Runnable{

	private volatile Thread thread = null;
	volatile boolean running = false;
	final Server server;
	final int portNumber;
	ConcurrentMap<String, Connection> userConnectionMap = new ConcurrentHashMap<String, Connection>();
	ConcurrentMap<String, ChatRoom> userRoomMap = new ConcurrentHashMap<String, ChatRoom>();
	ConcurrentMap<Connection, String> connectionUserMap = new ConcurrentHashMap<Connection, String>();
	private ConcurrentMap<String, ChatRoom> chatRooms;
	private ChatRoom mainRoom;
	private String mainRoomName = "main";
	
	private final String INVITE_COMMAND = "/invite";
	private final String CREATE_ROOM_COMMAND  = "/create";
	private final String PRIVATE_MESSAGE_COMMAND  = "/private";
	private final String LIST_ROOMS_COMMAND  = "/list_rooms";
	private final String JOIN_ROOM_COMMAND  = "/join";
	private final String GET_MORE_MESSAGES_COMMAND  = "/get_more_messages";
	
	public ChatServer(int portNumber) {
		mainRoom = new ChatRoom(mainRoomName);
		this.chatRooms = new ConcurrentHashMap<>();
		this.chatRooms.put(mainRoomName, mainRoom);
		this.server = new Server();
		
		this.portNumber = portNumber;
		KryoUtil.registerKryoClasses(server.getKryo());
		registerListener();
	}
	
	private void inviteUserToChatRoom(String roomName, String userNameInvited, String userNameInvitee) {
		ChatRoom room = chatRooms.get(roomName);
		if(room != null) {
			sendPrivateMessage(null, userNameInvited, "You got invited to the chatRoom " + roomName + " by user " + userNameInvitee, "Server: ");
		}
		else {
			System.out.println("The room doesnt exist");
		}
	}
	
	private void createChatRoom(String roomName) {
		chatRooms.put(roomName, new ChatRoom(roomName));
		System.out.println("successfully created chat room " + roomName);
	}
	private void sendPrivateMessage(Connection exception, String targetUserName, String message, String senderUserName) {
	    Connection targetConnection = userConnectionMap.get(targetUserName);

	    if (targetConnection != null && targetConnection.isConnected() && targetConnection != exception) {
	        // Create a private message object or use the appropriate method to send the message
	    	ChatMessage privateMessage = new ChatMessage(senderUserName, message);
	        targetConnection.sendTCP(privateMessage);
	    }
	}
	
	private void listRooms(String userName) {
    	StringBuilder sb = new StringBuilder();
    	chatRooms.forEach((userNameRoom, chatRoom) -> {
        	sb.append("Room Name: " + chatRoom.getRoomName() + "\n");
        });
        sendPrivateMessage(null, userName, sb.toString(), "Server: ");
	}
	
	private void joinRoom(String roomName, String userName) {
		ChatRoom room = chatRooms.get(roomName);
		if(room != null) {
			userRoomMap.put(userName, room);
			List<ChatMessage> history = room.lastFiveMessages();
	        sendPrivateMessage(null, userName, history.toString(), "Server: ");
		}
		else {
			System.out.println("The room doesnt exist");
		}
	}
	
	private void registerListener() {
		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof Login) {
					Login login = (Login)object;
					newUserLogged(login, connection);
					connection.sendTCP(new InfoMessage("Hello "+login.getUserName()));
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;
				}
				
				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage)object;
				    String userRoomName = userRoomMap.get(chatMessage.getUser()).getRoomName();
				    String chatMessageText = chatMessage.getTxt();
					System.out.println(chatMessage.getUser()+":" + chatMessageText);
					if(chatMessageText.startsWith(INVITE_COMMAND)) {
				        // Split the input string by whitespace
				        String[] parts = chatMessageText.split("\\s+");

				        // Check if the input has the expected format
				        if (parts.length == 3) {
				            String roomName = parts[1];
				            String userName = parts[2];
				            if(chatMessage.getUser() != userName) {
				            	inviteUserToChatRoom(roomName, userName, chatMessage.getUser());
				            }
				            else {
				            	System.out.println("You cannot invite yourself");
				            }
				        } else {
				            System.out.println("Invalid invite format, the expected format is /invite room_name user_name");
				        }
					}
					else if(chatMessageText.startsWith(CREATE_ROOM_COMMAND)) {
				        String[] parts = chatMessageText.split("\\s+");

				        // Check if the input has the expected format
				        if (parts.length == 2) {
				            String roomName = parts[1];
				            
				            createChatRoom(roomName);
				        } else {
				            System.out.println("Invalid create format, the expected format is /create room_name");
				        }
					}
					else if(chatMessageText.startsWith(PRIVATE_MESSAGE_COMMAND)) {
				        String[] parts = chatMessageText.split("\\s+", 3);

				        // Check if the input has the expected format
				        if (parts.length == 3) {
				            String userName = parts[1];
				            String message = parts[2];
				            sendPrivateMessage(connection, userName, message, chatMessage.getUser());
				        } else {
				            System.out.println("Invalid private message format, the expected format is /private user_name message");
				        }
					}
					else if(chatMessageText.startsWith(LIST_ROOMS_COMMAND)) {
						listRooms(chatMessage.getUser());
					}
					else if(chatMessageText.startsWith(JOIN_ROOM_COMMAND)) {
				        String[] parts = chatMessageText.split("\\s+", 2);

				        // Check if the input has the expected format
				        if (parts.length == 2) {
				            String roomName = parts[1];
				            joinRoom(roomName, chatMessage.getUser());
				        } else {
				            System.out.println("Invalid private message format, the expected format is /private user_name message");
				        }
					}
					else if(chatMessageText.startsWith(GET_MORE_MESSAGES_COMMAND)) {
				        String[] parts = chatMessageText.split("\\s+", 2);

				        // Check if the input has the expected format
				        if (parts.length == 2) {
				            String roomName = parts[1];
				            ChatRoom room = chatRooms.get(roomName);
							List<ChatMessage> history = room.getMoreMessages();
					        sendPrivateMessage(null, chatMessage.getUser(), history.toString(), "Server: ");
				        } else {
				            System.out.println("Invalid private message format, the expected format is /private user_name message");
				        }
					}
					else {
						broadcastChatMessage(chatMessage, connection, userRoomName); 
					}
					return;
				}

				if (object instanceof WhoRequest) {
					ListUsers listUsers = new ListUsers(getAllUsers());
					connection.sendTCP(listUsers);
					return;
				}
			}
			
			public void disconnected(Connection connection) {
				String user = connectionUserMap.get(connection);
				connectionUserMap.remove(connection);
				userConnectionMap.remove(user);
				showTextToAll(user+" has disconnected!", connection);
			}
		});
	}
	
	String[] getAllUsers() {
		String[] users = new String[userConnectionMap.size()];
		int i=0;
		for (String user: userConnectionMap.keySet()) {
			users[i] = user;
			i++;
		}
		
		return users;
	}
	void newUserLogged(Login loginMessage, Connection conn) {
		userConnectionMap.put(loginMessage.getUserName(), conn);
		connectionUserMap.put(conn, loginMessage.getUserName());
		userRoomMap.put(loginMessage.getUserName(), mainRoom);
		mainRoom.addNewUser(loginMessage.getUserName());
		showTextToAll("User "+loginMessage.getUserName()+" has connected!", conn);
	}
	private void broadcastChatMessage(ChatMessage message, Connection exception, String chatRoomName) {
	    for (Connection conn : userConnectionMap.values()) {
	        if (conn.isConnected() && conn != exception && isConnectionInRoom(conn, chatRoomName)) {
	            conn.sendTCP(message);
	            ChatRoom room = chatRooms.get(chatRoomName);
	            room.addMessageToHistory(message);
	        }
	    }
	}
	
	private boolean isConnectionInRoom(Connection connection, String chatRoomName) {
	    String userName = connectionUserMap.get(connection);
	    String userRoom = userRoomMap.get(userName).getRoomName();
//	    System.out.println("userName:" + userName);
//	    System.out.println("userRoom:" + userRoom);
//	    System.out.println("chatRoom:" + chatRoomName);
//	    System.out.println("userRoomMap:" + userRoomMap);
	    return userRoom != null && userRoom.equals(chatRoomName);
	}
	
	private void showTextToAll(String txt, Connection exception) {
		System.out.println(txt);
		for (Connection conn: userConnectionMap.values()) {
			if (conn.isConnected() && conn != exception)
				conn.sendTCP(new InfoMessage(txt));
		}
	}
	public void start() throws IOException {
		server.start();
		server.bind(portNumber);
		
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
	@Override
	public void run() {
		running = true;
		
		while(running) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	public static void main(String[] args) {
		
		if (args.length != 1) {
	        System.err.println("Usage: java -jar chatServer.jar <port number>");
	        System.out.println("Recommended port number is 54555");
	        System.exit(1);
	   }
	    
	   int portNumber = Integer.parseInt(args[0]);
	   try { 
		   ChatServer chatServer = new ChatServer(portNumber);
	   	   chatServer.start();
	   
			chatServer.thread.join();
	   } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	   } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	   }
	}
	
   
   
}
