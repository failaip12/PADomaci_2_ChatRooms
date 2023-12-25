package rs.raf.pds.v4.z5;

import java.util.ArrayList;
import java.util.List;

import rs.raf.pds.v4.z5.messages.ChatMessage;

public class ChatRoom {
	private String roomName;
	private ArrayList<String> userList;
	private List<ChatMessage> messageHistory;
	
	public ChatRoom(String roomName, ArrayList<String> userList, List<ChatMessage> messageHistory) {
		this.roomName = roomName;
		this.messageHistory = messageHistory;
		this.userList = userList;
	}
	
	public ChatRoom(String roomName) {
		this.roomName = roomName;
		this.userList =  new ArrayList<>();
		this.messageHistory = new ArrayList<>();
	}
	
	public void addMessageToHistory(ChatMessage message) {
		messageHistory.add(message);
	}
	
	public List<ChatMessage> lastFiveMessages() {
        int totalMessages = messageHistory.size();
        int startIndex = Math.max(0, totalMessages - 5);
        int endIndex = totalMessages;
        
        return messageHistory.subList(startIndex, endIndex);
	}
	
	public ArrayList<String> getUserList() {
		return userList;
	}

	public void setUserList(ArrayList<String> userList) {
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
	
}
