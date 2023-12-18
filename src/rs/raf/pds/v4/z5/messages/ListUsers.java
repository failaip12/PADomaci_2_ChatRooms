package rs.raf.pds.v4.z5.messages;

public class ListUsers {
	String[] users;
	
	protected ListUsers() {
		
	}
	public ListUsers(String[] users) {
		this.users = users;
	}

	public String[] getUsers() {
		return users;
	}
	
	
}
