package rs.raf.pds.v4.z5.messages;

public class DeleteMessage {
	private ChatMessage message; 
    private String user;

	protected DeleteMessage() {
		
	}
	
    public DeleteMessage(ChatMessage message, String user) {
    	this.message = message;
        this.user = user;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public String getUser() {
        return user;
    }
}



