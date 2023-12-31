package rs.raf.pds.v4.z5.messages;


public class EditMessage {

	private ChatMessage message; 
    private String user;
    private String txt;

	protected EditMessage() {
		
	}
	
    public EditMessage(ChatMessage message, String user, String txt) {
    	this.message = message;
        this.user = user;
        this.txt = txt;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public String getUser() {
        return user;
    }

    public String getTxt() {
        return txt;
    }
}
