package rs.raf.pds.v4.z5.messages;

import java.util.UUID;

public class ChatMessage {
	private String messageId; 
    private String user;
    private String txt;
    private boolean reply = false;
    private boolean edited = false;
    
    public boolean isEdited() {
		return edited;
	}

	private ChatMessage messageRepliendTo = null;

	public ChatMessage getMessageRepliendTo() {
		return messageRepliendTo;
	}

	public void setMessageRepliendTo(ChatMessage messageRepliendTo) {
		this.messageRepliendTo = messageRepliendTo;
	}

	public boolean isReply() {
		return reply;
	}

	public ChatMessage() {
		
	}
	
    public ChatMessage(UUID messageId, String user, String txt) {
    	this.messageId = messageId.toString();
        this.user = user;
        this.txt = txt;
    }

    public void setReply() {
    	this.reply = true;
    }
    
    public void setEdited() {
    	this.edited = true;
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
		if(user!=null) {
			return user.toString() + ": " + txt.toString() + "\n";
		}
		else {
			return txt.toString() + "\n";
		}
    }
}
