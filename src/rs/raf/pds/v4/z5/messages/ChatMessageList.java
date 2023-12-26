package rs.raf.pds.v4.z5.messages;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageList {
	private List<ChatMessage> messageList;

	public ChatMessageList() {
		this.messageList = new ArrayList<>();
	}

	public List<ChatMessage> getMessageList() {
		return messageList;
	}

	public ChatMessageList(List<ChatMessage> messageList) {
		this.messageList = new ArrayList<>(messageList);
	}
	
}
