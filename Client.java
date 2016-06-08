mport java.io.*;
import java.net.*;
public class Client extends Socket{
	public BufferedReader user_in;
	public PrintWriter server_out;
    public BufferedReader server_in;

	public Client(String IPaddress, int port) throws IOException{
		super(IPaddress, port);

		try {
			attachShutDownHook();
			user_in = new BufferedReader(new InputStreamReader(System.in,"UTF8"));
			//user_in denotes input from keyborads
			server_out = new PrintWriter(getOutputStream(),true);
			//server_out denotes output to Server
			server_in = new BufferedReader(new InputStreamReader(getInputStream(),"UTF8"));
			//sever in denotes input from Server
			new ReceiveMessage();
			String readline;
			readline = user_in.readLine();
			//to continually receive message from user and send to Server
			while(true) {
				server_out.println(readline);
				server_out.flush();
				if (readline.equals("logout")) {
					break;
				}
				// handle the normal logout, Attentional: here we still have to send logout the Server.
				readline = user_in.readLine();
			}
			user_in.close();
			server_out.close();
			server_in.close();
			close();
		} catch (IOException e) {
		} finally {
			close();
		}
	}

	public static void main(String[] args) throws IOException{
		int port = Integer.parseInt(args[1]);
		String IPaddress = args[0];
		new Client(IPaddress,port);
		//set up new CLient Socket
	}

    public void attachShutDownHook() {   
    	// handle the ctrl+c logout gracefully
	    Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
			    try {
				    server_out.println("logout");   
				    // above statement is most crucial, update the corresponding of User class
				    server_out.flush();
				    user_in.close();
				    server_out.close();
				    server_in.close();
				    close();
			    } catch (IOException e) {
			    }
		    }
	    });
    }

	class ReceiveMessage extends Thread {
		public ReceiveMessage() throws IOException{
			start();
		}
	    public void run() {  
	    // to continually receive and display message from the Server 
	    	try{
	    		String readline;
	    	    readline = server_in.readLine();
	    	    while (true) {
	    	    	if (readline.equals("logout")) {
	    	    		break;
	    	    	}
	    	    	// handle the timer logout, means 30 mins TIME_OUT logout
	    	    System.out.println(readline);
	    	    readline = server_in.readLine();
	    	    }
	    	    user_in.close();
			    server_out.close();
			    server_in.close();
			    close();
	    	} catch (IOException e) {
	    	}
	    }
	}
}
