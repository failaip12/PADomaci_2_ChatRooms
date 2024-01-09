package rs.raf.pds.v4.z5.messages;

import java.util.UUID;


public class ChatMessage{
	private String messageId; 
    private String sender;
    private String reciever = null;
    private String roomName = null;
	private String txt;
    private boolean reply = false;
    private boolean edited = false;
    private boolean privateMessage = false;

	private ChatMessage messageRepliedTo = null;


	protected ChatMessage() {
		
	}
	
    public ChatMessage(UUID messageId, String user, String txt) {
    	this.messageId = messageId.toString();
        this.sender = user;
        this.txt = txt;
    }
	
    public boolean isEdited() {
		return edited;
	}
    
    public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public boolean isPrivateMessage() {
		return privateMessage;
	}

    public String getReciever() {
		return reciever;
	}

	public void setReciever(String reciever) {
		this.reciever = reciever;
	}

	public ChatMessage getMessageRepliedTo() {
		return messageRepliedTo;
	}

	public void setMessageRepliedTo(ChatMessage messageRepliedTo) {
		this.messageRepliedTo = messageRepliedTo;
	}

	public boolean isReply() {
		return reply;
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

    public String getSender() {
        return sender;
    }

    public String getTxt() {
        return txt;
    }
    
    public void setTxt(String txt) {
		this.txt = txt;
	}

	@Override
    public String toString() {
		if(sender!=null) {
			if(roomName != null && !privateMessage) {
				return "(" + roomName + ") " + sender + ": " + txt + "\n";
			}
			return sender + ": " + txt + "\n";
		}
		else {
			return txt + "\n";
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
