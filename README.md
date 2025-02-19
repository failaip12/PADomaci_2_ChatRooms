# Java Network Chat Application

A feature-rich networked chat application built with Java, JavaFX, and KryoNet. This application supports multiple chat rooms, private messaging, and message management features.

## Features

- Real-time chat communication
- Multiple chat room support
- Private messaging between users
- Message management:
  - Edit messages
  - Delete messages
  - Reply to messages
- Message history tracking
- GUI interface built with JavaFX
- Efficient network communication using KryoNet

## Prerequisites

- Java 11 or higher
- JavaFX
- KryoNet 2.22.9
- Eclipse IDE (recommended)

## Project Structure

The project consists of several key components:

1. **Server**: Handles all client connections and message routing

2. **Client**: Manages connection to server and message sending/receiving

3. **GUI Client**: Provides user interface for chat functionality

## Setup and Running

1. **Server Setup**:
   - Navigate to the `jars/z5` directory
   - Run `startChatServerBat.bat` or execute:

   ```bash
   java -cp "../../lib/kryonet-2.22.9.main.jar;chatServer.jar" --add-opens=java.base/java.util=ALL-UNNAMED rs.raf.pds.v4.z5.ChatServer 4555
   ```

2. **Client Setup**:
   - For GUI Client:
     - Set the PATH_TO_FX environment variable to your JavaFX SDK path
     - Run `startChatClientGUI.bat` or execute:

   ```bash
   java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml -cp "../../lib/kryonet-2.22.9.main.jar;chatClient.jar;chatClientGUI.jar" --add-opens=java.base/java.util=ALL-UNNAMED rs.raf.pds.v4.z5.ChatClientGUI
   ```

## Usage

1. Start the server with desired port (default: 4555)
2. Launch the GUI client
3. Enter username, hostname (default: localhost), and port number
4. Connect to start chatting

### Chat Commands

- `/create [room_name]` - Create a new chat room
- `/invite [username]` - Invite user to current chat room
- `/private [username] [message]` - Send private message
- `/list_rooms` - List available chat rooms
- `/join [room_name]` - Join a specific chat room
- `/join_private_chat [username]` - Start private chat with user
- `/get_more_messages [room_name] [number]` - Load more message history

## Building

The project uses Eclipse for building. JAR files are generated using Eclipse's export functionality with the provided `.jardesc` files.

## Dependencies

- KryoNet 2.22.9 (included in `/lib` directory)
- JavaFX (requires separate installation)

## License

This project is open source and available under the MIT License.
