package rs.raf.pds.v4.z5;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.UpdatedChatMessage;

public class ChatRoom {
	private String roomName;
	private String roomCreator;
	private Set<String> userList;
	private List<ChatMessage> messageHistory; //This should probably be a set
	
	public ChatRoom(String roomName, Set<String> userList, List<ChatMessage> messageHistory) {
		this.roomName = roomName;
		this.messageHistory = messageHistory;
		this.userList = userList;
	}
	
	public ChatRoom(String userName, String roomName) {
		this.roomName = roomName;
		this.roomCreator = userName;
		this.userList =  new HashSet<String>();
		this.addNewUser(userName);
		this.messageHistory = new ArrayList<>();
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
	
	public ChatMessage getMessageFromUUID(UUID idMessage) {
		for(ChatMessage message:messageHistory) {
			if(message.getMessageId().equals(idMessage)) {
				return message;
			}
		}
		return null;
	}
	
	public List<ChatMessage> lastFiveMessages() {
        return getLastXMessages(5);
	}
	
	public List<ChatMessage> getLastXMessages(int limit) {
	    int totalMessages = messageHistory.size();
	    int startIndex = Math.max(0, totalMessages - limit);
	    int endIndex = Math.min(totalMessages, startIndex + limit); // Ensure endIndex doesn't exceed the size

	    return messageHistory.subList(startIndex, endIndex);
	}

	
	public Set<String> getUserList() {
		return userList;
	}

	public void setUserList(Set<String> userList) {
		this.userList = userList;
	}

	public List<ChatMessage> getMessageHistory() {
		return messageHistory;
	}

	public void setMessageHistory(List<ChatMessage> messageHistory) {
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
	
}
