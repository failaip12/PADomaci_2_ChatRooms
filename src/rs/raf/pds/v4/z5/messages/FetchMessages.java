package rs.raf.pds.v4.z5.messages;

public class FetchMessages {
		ChatMessage message;
		String userName;
		protected FetchMessages() {
			
		}
		public FetchMessages(ChatMessage message, String userName) {
			this.message = message;
			this.userName = userName;
		}

		public ChatMessage getMessage() {
			return message;
		}
		
		public String getUserName() {
			return userName;
		}
		
}
