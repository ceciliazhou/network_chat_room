/**
 * This program models a server for a 1-to-N Server-client architechture based chatting room.
 * To use this tool, please follow the instructions as follows:
 *
 * 1. start the ChatServer program on the server host.
 *    USAGE: java ChatServer port
 *           optional command line arguments:
 *               [port]: the port number to listen and accept the incoming connection request, 8000 by default.
 *
 * 2. start the ChatClient programs on the client hosts.
 *    USAGE: java ChatClient serverHost serverPort user
 *           optional command line arguments:
 *               [serverHost]: the IP or host name of the server to be connected to, "localhost" by default.
 *               [serverPort]: the port number the server is listening to, 8000 by default.
 *               [user]: the user using the chatting client, "anonymous_user" by default.
 *
 * NOTE: The client will fail to connect if the server is not turned on or wrong server host and port number are given.
 *
 *@author: Cecilia
 */
 
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * ChatServer models a server for a 1-to-N Server-client architechture based chatting room.
 * It will sit on a specified port number waiting for clients to request connections.
 * Once a client is connected to this server, it will keep recieving all the message sent from all connected clients.
 */
public class ChatServer {
	public static void main(String[] args) {
		int port = (args.length > 0) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
		new ChatServer().startWorking(port);	
	}
	
	public void startWorking(final int port){
		new Thread(){ public void run(){ startConnectionAcceptor(port); }}.start();
        new Thread(){ public void run(){ startMessageDistributor(); }}.start();			
	}
	
	/**
	 * Keep removing messages from the message queue and sending them to all the clients currently connected.
     */	 	
	private void startMessageDistributor() { 
		while(true) {
			Socket client = null;
			try {
				String msg = msgQueue.take();
				synchronized(this){
					for(int i = 0; i < connections.size(); i++) {
						client = connections.get(i);
						try{
							new PrintWriter(client.getOutputStream(), true).println(msg);
						} 
						catch (IOException e) {
							client.close();
							removeClient(client);
						} 
					}
				}
			}
			catch (IOException e) {
			} 
			catch (InterruptedException e){
			} 
		}
	}
	
	/** 
	 * Create a server socket on the specified port and keep listening on it and accepting coming in connection request.
	 * @param port the port on which to start the server socket.
	 */
	private void startConnectionAcceptor(int port) {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Start service on [" + port + "] successfully!");
			
			while(true) {
				Socket clientSocket = serverSocket.accept();
				addClient(clientSocket);				
				new MessageCollector(clientSocket).start();
			}
		}
		catch(IOException e) {
				System.out.println("Failed to start service on port [ "+port+" ].\nPlease check whether port is available or choose another free port.");
				System.out.println(e.toString());
		}
		finally{
			if(serverSocket != null){
				try{
					serverSocket.close();
				}
				catch(IOException e) {
				}
			}
		}
	}
	
	/** 
	 * A Thread class to collect messages from a specified socket.
	 * @param clientSocket the client socket from which to collect messages.
	 */
	private class MessageCollector extends Thread { 
		/** 
		 * Construct a MessageCollector thread.
		 * @param clientSocket the client socket from which to collect messages.
		 */
		public MessageCollector(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}
		
		/** 
		 * Run the thread to collect messages coming from the client socket.
		 */
		 public void run() {
			try {					
				Scanner incomingMsg = new Scanner(clientSocket.getInputStream());
				while (incomingMsg.hasNextLine()){
					msgQueue.put(incomingMsg.nextLine());
				}
			} 
			catch (Exception e) {
			} 
			finally {
				if(!clientSocket.isClosed()){
					try {
						clientSocket.close();
					} 
					catch(IOException e) {
					}
					removeClient(clientSocket);
				}
			}	
		}	
		
		private Socket clientSocket;
	}	
	
	/** 
	 * Remove a client socket from the connection set.
	 * @param clientSocket the client socket to be removed.
	 */
	private synchronized void removeClient(Socket clientSocket){
		connections.remove(clientSocket); 
	}

	/** 
	 * Add a client socket to the connection set.
	 * @param clientSocket the client socket to be added.
	 */	
	private synchronized void addClient(Socket clientSocket){
		connections.add(clientSocket);
	}
	
	public static final int DEFAULT_PORT = 8000;
	private final ArrayList<Socket> connections = new ArrayList<Socket>();
	private final BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>();
	private final Lock connectionLock = new ReentrantLock();

}
