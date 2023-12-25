package rs.raf.pds.v4.z5.messages;

import java.util.UUID;

public class ChatMessage {
	private String messageId; 
    private String user;
    private String txt;

	protected ChatMessage() {
		
	}
	
    public ChatMessage(UUID messageId, String user, String txt) {
    	this.messageId = messageId.toString();
        this.user = user;
        this.txt = txt;
    }

    
    public ChatMessage(String user, String txt) {
        this(UUID.randomUUID(), user, txt);
    }

    public UUID getMessageId() {
        return UUID.fromString(messageId);
    }

    public String getUser() {
        return user;
    }

    public String getTxt() {
        return txt;
    }
    
    public void setTxt(String txt) {
		this.txt = txt;
	}

	@Override
    public String toString() {
    	return user.toString() + ": " + txt.toString() + "\n";
    }
}
