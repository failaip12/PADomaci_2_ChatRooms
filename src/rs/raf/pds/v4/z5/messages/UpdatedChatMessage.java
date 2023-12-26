package rs.raf.pds.v4.z5.messages;

import java.util.UUID;

public class UpdatedChatMessage {

	private String messageId; 
    private String user;
    private String txt;

	protected UpdatedChatMessage() {
		
	}
	
    public UpdatedChatMessage(UUID messageId, String user, String txt) {
    	this.messageId = messageId.toString();
        this.user = user;
        this.txt = txt;
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

	@Override
    public String toString() {
		return user.toString() + ": " + txt.toString() + "\n";
    }
}
