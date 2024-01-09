package rs.raf.pds.v4.z5.messages;

import com.esotericsoftware.kryo.Kryo;

import rs.raf.pds.v4.z5.ChatRoom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public class KryoUtil {
	public static void registerKryoClasses(Kryo kryo) {
		kryo.register(String.class);
		kryo.register(String[].class);
		kryo.register(HashSet.class);
		kryo.register(LinkedHashSet.class);
		kryo.register(List.class); //not needed?
		kryo.register(ArrayList.class);
		kryo.register(UUID.class);

		kryo.register(ChatMessage.class);
		kryo.register(ChatMessageLinkedHashSet.class);
		kryo.register(EditMessage.class);
		kryo.register(FetchMessages.class);
		kryo.register(InfoMessage.class);
		kryo.register(ListUsers.class);
		kryo.register(Login.class);
		kryo.register(UpdatedChatMessage.class);
		kryo.register(WhoRequest.class);
		
		kryo.register(ChatRoom.class);
	}
}
