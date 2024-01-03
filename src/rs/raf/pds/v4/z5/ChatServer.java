package rs.raf.pds.v4.z5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

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

public class ChatServer implements Listener, Runnable{

	private volatile Thread thread = null;
	volatile boolean running = false;
	final Server server;
	final int portNumber;
	ConcurrentMap<String, Connection> userConnectionMap = new ConcurrentHashMap<String, Connection>();
	ConcurrentMap<String, ChatRoom> userRoomMap = new ConcurrentHashMap<String, ChatRoom>();
	ConcurrentMap<Connection, String> connectionUserMap = new ConcurrentHashMap<Connection, String>();
	ConcurrentMap<Connection, Boolean> connectionBoolMap = new ConcurrentHashMap<Connection, Boolean>();
	private ConcurrentMap<String, ChatRoom> chatRooms;
	private ConcurrentMap<String, ChatRoom> privateChatRooms;
	private ChatRoom mainRoom;
	private String mainRoomName = "main";
	
	public ChatServer(int portNumber) {
		mainRoom = new ChatRoom("Server: ", mainRoomName);
		this.chatRooms = new ConcurrentHashMap<String, ChatRoom>();
		this.privateChatRooms = new ConcurrentHashMap<String, ChatRoom>();
		this.chatRooms.put(mainRoomName, mainRoom);
		this.server = new Server();
		
		this.portNumber = portNumber;
		KryoUtil.registerKryoClasses(server.getKryo());
		registerListener();
	}
	
	private void inviteUserToChatRoom(Connection connSender, String roomName, String userNameInvited, String userNameInvitee) {
		ChatRoom room = chatRooms.get(roomName);
		if(room != null) {
			Connection connInvited = userConnectionMap.get(userNameInvited);
			if(connInvited != null && connInvited.isConnected()) {
				InfoMessage infoMessageInvited = new InfoMessage("You got invited to the chatRoom " + roomName + " by user " + userNameInvitee);
				connInvited.sendTCP(infoMessageInvited);
				
				room.addNewUser(userNameInvited);
				InfoMessage infoMessageSender = new InfoMessage("Successfully invited the user " + userNameInvited + " to the ChatRoom " + roomName);
				connSender.sendTCP(infoMessageSender);
			}
			else {
				InfoMessage infoMessageInvitee = new InfoMessage("The user doesnt exist/isn't online");
				connSender.sendTCP(infoMessageInvitee);
			}

		}
		else {
			InfoMessage infoMessageInvitee = new InfoMessage("The room doesnt exist");
			connSender.sendTCP(infoMessageInvitee);
		}
	}
	
	private void createChatRoom(Connection connSender, String roomName) {
		String user = connectionUserMap.get(connSender);
		if(chatRooms.get(roomName) == null) {
			chatRooms.put(roomName, new ChatRoom(user, roomName));
			InfoMessage infoMessageSender = new InfoMessage("Successfully created chat room " + roomName);
			connSender.sendTCP(infoMessageSender);
		}
		else {
			InfoMessage infoMessageSender = new InfoMessage("A room with that name already exists");
			connSender.sendTCP(infoMessageSender);
		}
	}
	
    private ChatRoom getPrivateChatRoom(String user1, String user2) {
        String key = generateKey(user1, user2);

        // Retrieve or create a chat room based on the key
        ChatRoom pRoom = new ChatRoom(user1, key);
        pRoom.addNewUser(user1);
        pRoom.addNewUser(user2);
        return privateChatRooms.computeIfAbsent(key, k -> pRoom);
    }

    private String generateKey(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + ":" + user2 : user2 + ":" + user1;
    }
	
	private void sendPrivateMessage(Connection exception, String targetUserName, String message, String senderUserName) {
	    Connection targetConnection = userConnectionMap.get(targetUserName);
	    if(targetConnection == exception) {
			InfoMessage infoMessageSender = new InfoMessage("You cannot send a private message to yourself");
			exception.sendTCP(infoMessageSender);
			return;
	    }
	    if (targetConnection != null && targetConnection.isConnected() && targetConnection != exception) {
	        // Create a private message object or use the appropriate method to send the message
	    	ChatMessage privateMessage = new ChatMessage(senderUserName, message);
	    	privateMessage.setPrivateMessage();
	    	privateMessage.setReciever(targetUserName);
    		ChatRoom pChatRoom = getPrivateChatRoom(targetUserName, senderUserName);
    		pChatRoom.addMessageToHistory(privateMessage);
	    	targetConnection.sendTCP(privateMessage);
	        exception.sendTCP(privateMessage);
	    }
	    else {
			InfoMessage infoMessageSender = new InfoMessage("The user doesnt exist/isn't online");
			exception.sendTCP(infoMessageSender);
	    }
	}
	private void sendPrivateMessageReply(Connection senderConnection, Connection targetConnection, ChatMessage message) {
	    if (targetConnection != null && targetConnection.isConnected() && senderConnection != null && senderConnection.isConnected()) {
	    	ChatMessage privateMessage = new ChatMessage(message.getSender(), message.getTxt());
	    	privateMessage.setPrivateMessage();
	    	privateMessage.setReciever(connectionUserMap.get(targetConnection));
	    	privateMessage.setReply();
	    	privateMessage.setMessageRepliedTo(message.getMessageRepliedTo());
    		ChatRoom pChatRoom = getPrivateChatRoom(message.getSender(), connectionUserMap.get(targetConnection));
    		pChatRoom.addMessageToHistory(privateMessage);
	        targetConnection.sendTCP(privateMessage);
	        senderConnection.sendTCP(privateMessage);
	    }
	    else {
			InfoMessage infoMessageSender = new InfoMessage("erroR");
			senderConnection.sendTCP(infoMessageSender);
	    }
	}
	private void listRooms(Connection connSender, String userName) {
    	StringBuilder sb = new StringBuilder();
    	chatRooms.forEach((userNameRoom, chatRoom) -> {
        	sb.append(" Room Creator: " + chatRoom.getRoomCreator() + " Room Name: " + chatRoom.getRoomName() + "\n");
        });
		InfoMessage infoMessageSender = new InfoMessage(sb.toString());
		connSender.sendTCP(infoMessageSender);
	}
	
	private void joinPrivateRoom(Connection connSender, String userNameSender, String userNameReciever) {
		ChatRoom room = getPrivateChatRoom(userNameSender, userNameReciever);
		if(room != null) {
			if(room.userInRoom(userNameSender)) {
				userRoomMap.put(userNameSender, room);
				InfoMessage infoMessage = new InfoMessage("Successfully joined the private room " + userNameReciever);
				connSender.sendTCP(infoMessage);
				connSender.sendTCP(new ChatMessageLinkedHashSet (room.getMessageHistory()));
			}
			else {
				InfoMessage infoMessageSender = new InfoMessage("You aren't invited to this room!");
				connSender.sendTCP(infoMessageSender);
			}
		}
		else {
			InfoMessage infoMessageSender = new InfoMessage("The room doesn't exist");
			connSender.sendTCP(infoMessageSender);
		}
	}
	
	private void joinRoom(Connection connSender, String roomName, String userName) {
		ChatRoom room = chatRooms.get(roomName);
		if(room != null) {
			if(room.userInRoom(userName)) {
				userRoomMap.put(userName, room);
				InfoMessage infoMessage = new InfoMessage("Successfully joined the room " + roomName);
				connSender.sendTCP(infoMessage);
				connSender.sendTCP(new ChatMessageLinkedHashSet (room.lastFiveMessages()));
			}
			else {
				InfoMessage infoMessageSender = new InfoMessage("You aren't invited to this room!");
				connSender.sendTCP(infoMessageSender);
			}
		}
		else {
			InfoMessage infoMessageSender = new InfoMessage("The room doesn't exist");
			connSender.sendTCP(infoMessageSender);
		}
	}
	
	private void registerListener() {
		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof Login) {
					Login login = (Login)object;
					if(newUserLogged(login, connection)) {
						connection.sendTCP(new InfoMessage("Hello "+login.getUserName()));
						connection.sendTCP(new ChatMessageLinkedHashSet (mainRoom.lastFiveMessages()));
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					else {
						connectionBoolMap.put(connection, true);
						connection.close();
					}
					return;
				}
				
				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage)object;
				    String userRoomName = userRoomMap.get(chatMessage.getSender()).getRoomName();
				    String chatMessageText = chatMessage.getTxt();
					System.out.println(chatMessage.getSender()+":" + chatMessageText);
					if(chatMessage.isReply() && chatMessage.getMessageRepliedTo().isPrivateMessage()) {
						Connection connSender = userConnectionMap.get(chatMessage.getSender());
						Connection conn2 = userConnectionMap.get(chatMessage.getMessageRepliedTo().getReciever());
						if(connSender.equals(conn2)) {
							conn2 = userConnectionMap.get(chatMessage.getMessageRepliedTo().getSender());
						}
						sendPrivateMessageReply(connSender, conn2, chatMessage);
					}
					else if(chatMessageText.startsWith(Constants.INVITE_COMMAND)) {
				        // Split the input string by whitespace
				        String[] parts = chatMessageText.split("\\s+");

				        // Check if the input has the expected format
				        if (parts.length == 3) {
				            String roomName = parts[1];
				            String userName = parts[2].trim();
				            if(chatMessage.getSender() != userName) {
				            	inviteUserToChatRoom(connection, roomName, userName, chatMessage.getSender());
				            }
				            else {
				    			InfoMessage infoMessageSender = new InfoMessage("You cannot invite yourself");
				    			connection.sendTCP(infoMessageSender);
				            }
				        } else {
			    			InfoMessage infoMessageSender = new InfoMessage("Invalid invite format, the expected format is /invite room_name user_name");
			    			connection.sendTCP(infoMessageSender);
				        }
					}
					else if(chatMessageText.startsWith(Constants.CREATE_ROOM_COMMAND)) {
				        String[] parts = chatMessageText.split("\\s+");

				        // Check if the input has the expected format
				        if (parts.length == 2) {
				            String roomName = parts[1];
				            
				            createChatRoom(connection, roomName);
				        } else {
			    			InfoMessage infoMessageSender = new InfoMessage("Invalid create format, the expected format is /create room_name");
			    			connection.sendTCP(infoMessageSender);
				        }
					}
					else if(chatMessageText.startsWith(Constants.PRIVATE_MESSAGE_COMMAND)) {
				        String[] parts = chatMessageText.split("\\s+", 3);

				        // Check if the input has the expected format
				        if (parts.length == 3) {
				            String userName = parts[1];
				            String message = parts[2];
				            sendPrivateMessage(connection, userName, message, chatMessage.getSender());
				        } else {
			    			InfoMessage infoMessageSender = new InfoMessage("Invalid private message format, the expected format is /private user_name message");
			    			connection.sendTCP(infoMessageSender);
				        }
					}
					else if(chatMessageText.startsWith(Constants.LIST_ROOMS_COMMAND)) {
						listRooms(connection, chatMessage.getSender());
					}
					else if(chatMessageText.startsWith(Constants.JOIN_PRIVATE_CHAT_COMMAND)) {
				        String[] parts = chatMessageText.split("\\s+", 2);

				        // Check if the input has the expected format
				        if (parts.length == 2) {
				            String userName = parts[1].trim();
				            if(!userName.equals(chatMessage.getSender())) {
				            	joinPrivateRoom(connection, chatMessage.getSender(), userName);
				            }
				            else {
				    			InfoMessage infoMessageSender = new InfoMessage("You cannot join your own private chatroom, choose another user please.");
				    			connection.sendTCP(infoMessageSender);
				            }
				        } else {
			    			InfoMessage infoMessageSender = new InfoMessage("Invalid join private chat format, the expected format is /join_private_chat username");
			    			connection.sendTCP(infoMessageSender);
				        }
					}
					else if(chatMessageText.startsWith(Constants.JOIN_ROOM_COMMAND)) {
				        String[] parts = chatMessageText.split("\\s+", 2);

				        // Check if the input has the expected format
				        if (parts.length == 2) {
				            String roomName = parts[1];
				            joinRoom(connection, roomName, chatMessage.getSender());
				        } else {
			    			InfoMessage infoMessageSender = new InfoMessage("Invalid join room format, the expected format is /join room_name");
			    			connection.sendTCP(infoMessageSender);
				        }
					}
					else if(chatMessageText.startsWith(Constants.GET_MORE_MESSAGES_COMMAND)) {
				        String[] parts = chatMessageText.split("\\s+", 3);
				        int numberOfMessages = 0;
				        // Check if the input has the expected format
				        if (parts.length == 3) {
				            String roomName = parts[1];
				            try {
				            	numberOfMessages = Integer.parseInt(parts[2]);
				            }
				            catch (NumberFormatException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
				            	InfoMessage infoMessageSender = new InfoMessage("number_of_message has to be a valid integer");
								connection.sendTCP(infoMessageSender);
								return;
							}
				            ChatRoom room = chatRooms.get(roomName);
				            if(room != null) {
					            if(room.userInRoom(chatMessage.getSender())) {
					            	connection.sendTCP(new ChatMessageLinkedHashSet (room.getLastXMessages(numberOfMessages)));
					            }
					            else {
									InfoMessage infoMessageSender = new InfoMessage("You aren't invited to this room!");
									connection.sendTCP(infoMessageSender);
					            }
				            }
				            else {
								InfoMessage infoMessageSender = new InfoMessage("The room doesn't exist");
								connection.sendTCP(infoMessageSender);
				            }
				        } else {
			    			InfoMessage infoMessageSender = new InfoMessage("Invalid get more messages format, the expected format is /get_more_messages room_name number_of_messages");
			    			connection.sendTCP(infoMessageSender);
				        }
					}
					else {
						broadcastChatMessage(connection, chatMessage, userRoomName);
					}
					return;
				}
				if (object instanceof EditMessage) {
					EditMessage editMessage = (EditMessage)object;
					ChatMessage message = editMessage.getMessage();
					String user = editMessage.getUser();
					String messageText = editMessage.getTxt();
					if(message.isPrivateMessage()) {
			    		ChatRoom pChatRoom = getPrivateChatRoom(message.getSender(), message.getReciever());
						UpdatedChatMessage updatedMessage = new UpdatedChatMessage(message.getMessageId(), user, messageText);
			    		pChatRoom.editMessage(updatedMessage);
			    		userConnectionMap.get(message.getSender()).sendTCP(updatedMessage);
			    		userConnectionMap.get(message.getReciever()).sendTCP(updatedMessage);
					}
					else {
						ChatRoom room = userRoomMap.get(user);
						if(user.equals(message.getSender())) {
							UpdatedChatMessage updatedMessage = new UpdatedChatMessage(message.getMessageId(), user, messageText);
							room.editMessage(updatedMessage);
							broadcastUpdatedChatMessage(updatedMessage, room.getRoomName());
						}
						else {
							System.out.println("GRESKA");
						}
					}
					return;
				}
				if (object instanceof WhoRequest) {
					ListUsers listUsers = new ListUsers(getAllUsers());
					connection.sendTCP(listUsers);
					return;
				}
				if (object instanceof FetchMessages) {
					FetchMessages fetchMessages = (FetchMessages)object;
					ChatMessage message = fetchMessages.getMessage();
					ChatRoom room;
					if(message.isPrivateMessage()) {
						room = getPrivateChatRoom(message.getReciever(), message.getSender());
					}
					else {
						String roomName = userRoomMap.get(message.getSender()).getRoomName();
						room = chatRooms.get(roomName);
					}
					LinkedHashSet<ChatMessage> history = room.getMessageHistory();
					List<ChatMessage> messageList = new ArrayList<>(history);
					int index = messageList.lastIndexOf(message);
					connection.sendTCP(new ChatMessageLinkedHashSet (room.getLastXMessages(history.size() - index)));
					return;
				}
			}
			
			public void disconnected(Connection connection) {
				if(connectionBoolMap.get(connection) != null) {
					connectionBoolMap.remove(connection);
				}
				else {
					String user = connectionUserMap.get(connection);
					connectionUserMap.remove(connection);
					userConnectionMap.remove(user);
					showTextToAll(user+" has disconnected!", connection);
				}
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
	boolean newUserLogged(Login loginMessage, Connection conn) {
		if(userConnectionMap.get(loginMessage.getUserName()) == null) {
			userConnectionMap.put(loginMessage.getUserName(), conn);
			connectionUserMap.put(conn, loginMessage.getUserName());
			userRoomMap.put(loginMessage.getUserName(), mainRoom);
			mainRoom.addNewUser(loginMessage.getUserName());
			showTextToAll("User "+loginMessage.getUserName()+" has connected!", conn);
			return true;
		}
		else {
			InfoMessage infoMessage = new InfoMessage("You cannot use that username.");
			conn.sendTCP(infoMessage);
			return false;
		}
	}
	private void broadcastChatMessage(Connection exception, ChatMessage message, String chatRoomName) {
        ChatRoom room = chatRooms.get(chatRoomName);
        if(room != null) {
        	message.setRoomName(chatRoomName);
		    for (Connection conn : userConnectionMap.values()) {
		        if (conn.isConnected() && isConnectionInRoom(conn, chatRoomName)) {
		            conn.sendTCP(message);
		        }
		    }
        }
        else {
        	room = privateChatRooms.get(chatRoomName);
        	if(room != null) {
        		Set<String> users = room.getUserList();
	        	message.setPrivateMessage();
	            for (String user : users) {
	                if (!user.equals(message.getSender())) {
	    	        	message.setReciever(user);
	                }
	            }
        		for(String user:users) {
        			Connection conn = userConnectionMap.get(user);
				        if (conn.isConnected()) {
				            conn.sendTCP(message);
				        }
    			    }
        		}
        	else {
        		System.out.println("Greska");
        		return;
        	}
        }
        room.addMessageToHistory(message);
	}
	
	private void broadcastUpdatedChatMessage(UpdatedChatMessage message, String chatRoomName) {
	    for (Connection conn : userConnectionMap.values()) {
	        if (conn.isConnected() && isConnectionInRoom(conn, chatRoomName)) {
	            conn.sendTCP(message);
	        }
	    }
	}
	
	private void broadcastInfoMessageToRoom(InfoMessage message, String chatRoomName) {
	    for (Connection conn : userConnectionMap.values()) {
	        if (conn.isConnected() && isConnectionInRoom(conn, chatRoomName)) {
	            conn.sendTCP(message);
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
