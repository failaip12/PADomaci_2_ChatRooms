package rs.raf.pds.v4.z5.messages;

import java.util.LinkedHashSet;

public class ChatMessageLinkedHashSet {
	private LinkedHashSet<ChatMessage> messageList;

	public ChatMessageLinkedHashSet() {
		this.messageList = new LinkedHashSet<ChatMessage>();
	}

	public LinkedHashSet<ChatMessage> getMessageList() {
		return messageList;
	}

	public ChatMessageLinkedHashSet(LinkedHashSet<ChatMessage> messageList) {
		this.messageList = new LinkedHashSet<ChatMessage>(messageList);
	}
	
}
