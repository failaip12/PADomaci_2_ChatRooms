package rs.raf.pds.v4.z5;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.UpdatedChatMessage;

public class ChatRoom {
	private String roomName;
	private String roomCreator;
	private Set<String> userList;
	private LinkedHashSet<ChatMessage> messageHistory;
	private boolean private_chat = false;
	
	protected ChatRoom() {
		
	}
	
	public ChatRoom(String roomName, Set<String> userList, LinkedHashSet<ChatMessage> messageHistory) {
		this.roomName = roomName;
		this.messageHistory = messageHistory;
		this.userList = userList;
	}
	
	public ChatRoom(String userName, String roomName) {
		this.roomName = roomName;
		this.roomCreator = userName;
		this.userList =  new HashSet<String>();
		this.addNewUser(userName);
		this.messageHistory = new LinkedHashSet<ChatMessage>();
	}
	
	public boolean isPrivate_chat() {
		return private_chat;
	}

	public void setPrivate_chat() {
		this.private_chat = true;
	}
	
	public void addMessageToHistory(ChatMessage message) {
		messageHistory.add(message);
	}
	
	public boolean userInRoom(String userName) {
		return userList.contains(userName);
	}
	
	public void editMessage(UpdatedChatMessage updatedMessage) {
		for(ChatMessage message:messageHistory) {
			if(message.getMessageId().equals(updatedMessage.getMessageId())) {
				message.setTxt(updatedMessage.getTxt());
				message.setEdited();
			}
			if(message.isReply() && message.getMessageRepliedTo().getMessageId().equals(updatedMessage.getMessageId())) {
				message.getMessageRepliedTo().setTxt(updatedMessage.getTxt());
			}
		}
	}
	
	public void deleteMessage(ChatMessage deletedMessage) {
		for(ChatMessage message:messageHistory) {
			if(message.isReply() && message.getMessageRepliedTo().getMessageId().equals(deletedMessage.getMessageId())) {
				message.getMessageRepliedTo().setTxt("Original message was deleted");
			}
		}
		messageHistory.remove(deletedMessage);
	}
	
	public ChatMessage getMessageFromUUID(UUID idMessage) {
		for(ChatMessage message:messageHistory) {
			if(message.getMessageId().equals(idMessage)) {
				return message;
			}
		}
		return null;
	}
	
	public LinkedHashSet<ChatMessage> lastFiveMessages() {
        return getLastXMessages(5);
	}
	
	public LinkedHashSet<ChatMessage> getLastXMessages(int limit) {
	    int totalMessages = messageHistory.size();
	    int startIndex = Math.max(0, totalMessages - limit);

	    LinkedHashSet<ChatMessage> result = new LinkedHashSet<>();
	    Iterator<ChatMessage> iterator = messageHistory.iterator();
	    for (int i = 0; i < startIndex && iterator.hasNext(); i++) {
	        iterator.next();
	    }
	    for (int i = startIndex; i < totalMessages && i < startIndex + limit && iterator.hasNext(); i++) {
	        result.add(iterator.next());
	    }
	    return result;
	}

	
	public Set<String> getUserList() {
		return userList;
	}

	public void setUserList(Set<String> userList) {
		this.userList = userList;
	}

	public LinkedHashSet<ChatMessage> getMessageHistory() {
		return messageHistory;
	}

	public void setMessageHistory(LinkedHashSet<ChatMessage> messageHistory) {
		this.messageHistory = messageHistory;
	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}
	
	public void addNewUser(String username) {
		this.userList.add(username);
	}

	public String getRoomCreator() {
		return roomCreator;
	}
	
	@Override
    public String toString() {
		return roomName;
    }
	
}
