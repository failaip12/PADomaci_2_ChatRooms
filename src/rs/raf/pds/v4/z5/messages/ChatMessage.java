package rs.raf.pds.v4.z5.messages;

import java.util.UUID;


public class ChatMessage{
	private String messageId; 
    private String user;
    private String txt;
    private boolean reply = false;
    private boolean edited = false;
    private boolean privateMessage = false;
    
    public boolean isEdited() {
		return edited;
	}
    
    public boolean isPrivateMessage() {
		return privateMessage;
	}
    
	private ChatMessage messageRepliedTo = null;

	public ChatMessage getMessageRepliedTo() {
		return messageRepliedTo;
	}

	public void setMessageRepliedTo(ChatMessage messageRepliedTo) {
		this.messageRepliedTo = messageRepliedTo;
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
    
    public void setPrivateMessage() {
    	this.privateMessage = true;
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
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj) {
	        return true;
	    }
	    if (obj == null || getClass() != obj.getClass()) {
	        return false;
	    }

	    ChatMessage message = (ChatMessage) obj;
		return this.getMessageId().equals(message.getMessageId());
	}
}
