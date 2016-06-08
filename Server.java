import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
public class Server extends ServerSocket{
	public static ArrayList<User> User_List = new ArrayList<User>();
	// the List to store data of users 
	public static int VisitTimes;
	// the parameter to count digest rate

	class User {
		public String Name;
		public ServerThread Thread;
		public long LoggoutTime;
		public ArrayList<String> OfflineMsg;
	}
	// the class User include name, socket, logouttime (wholast need), offlinemsg (help send offline msg)

	public Server(int port) throws IOException{
		super(port);

		try {
			attachShutDownHook();
			while (true) {
				Socket socket = accept();
				new ServerThread(socket);
				//continually receive new client and create new socket for them
			}
		} catch (IOException e) {
		} finally {
			close();
		}
	}

	public void attachShutDownHook() {
		//handle the gracefully exit of ctrl + c
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					for (int i = 0; i < User_List.size(); i++) {
						ServerThread client = User_List.get(i).Thread;
						client.SendMessage("Sorry, the server is closed.");
						//send the information that server is to be closed.
						client.SendMessage("logout");
						//send the clients information to close the client-side socket.
					}
					User_List.clear();
					close();
				} catch (IOException e) {
				}
			}
		});
	}

	public static void main(String[] args) throws IOException{
		int port = Integer.parseInt(args[0]);
		new Server(port);
		//create the new SeverSocket.
	} 

    class ServerThread extends Thread {
	    public Socket client;
	    public BufferedReader client_in;
	    //clinet_in to denote the input from the client socket
	    public PrintWriter client_out;
	    //client_out denote the output to the client socket
	    public BufferedReader file_in;
	    // file_in denote the input from the user_pass.txt
	    public PrintWriter file_out;
	    // file_out denote the output to the txt, to modify the password
	    public String Username;
	    public User newUser = new User();
	    public int BLOCK_TIME = 10;
	    //BLOCK_TIME could be modified
	    public int TIME_OUT = 3;
	    //TIME_OUT could be modified

	    public ServerThread(Socket s) throws IOException{
		    client = s;
		    client_in = new BufferedReader(new InputStreamReader(client.getInputStream(),"UTF8"));
		    client_out = new PrintWriter(client.getOutputStream(),true);
		    start();
	    }

	    public boolean Verification() {
	    	String[] data = new String[2]; 
	    	int Wrongtimes = 0;
            try {
            	while (true) {
            		client_out.println("Input Username:");
            		String username = client_in.readLine();
            		for (int i = 0; i < User_List.size(); i++) {
            			// whether the user has logged in
            			if (username.equals(User_List.get(i).Name) 
            				&& User_List.get(i).LoggoutTime == 0) {
            				client_out.println("User has logged in.");
            				return false;
            			}
            		}
            		file_in = new BufferedReader(new InputStreamReader(new FileInputStream("user_pass.txt")));
            		String data_file = file_in.readLine();
            		while (data_file != null) {
            			data = data_file.split(" ");
            			if (username.equals(data[0])) {
            			// find the corresponding username in the txt file
            				break;
            			}
            			data_file = file_in.readLine();
            		}
            	    if (data_file == null) {
            	    	// if could not find it, ask user to reinput.
            	    	client_out.println("User not Found.");
            	    } else {
            	    	break;
            	    }
            	}
            	while (true) {
            		client_out.println("Input Password:");
            		String password = client_in.readLine();
            		if (password.equals(data[1])) {
            			// verify whether the username match the password.
            			client_out.println("Welcome to simple chat server!");
            			// let the user know the function of chat room.
				        client_out.println("Current commands: whoelse, wholast, history, reset password, broadcast, message");
            			Username = data[0];
            			file_in.close();
            			return true;
            		} else {
            			// if do not match, tell the user
            			client_out.println("Password is wrong!");
            			Wrongtimes ++;
            		} 
            		if (Wrongtimes == 3) {
            			// if wrongtimes is 3, block the terminal
            			client_out.println("You are not the user!");
            			client_out.println("Blocking time:" + BLOCK_TIME + " seconds");
            			this.sleep(1000 * BLOCK_TIME);
            			return false;            			
            		}
            	}
            } catch (Exception e) {
            	return false;
            }
	    }

	    public void SendMessage(String msg) {
	    	// assist to send message to the corresponidng client
	    	client_out.println(msg);
	    }

	    public void Broadcast(String msg) {
			for (int i = 0; i<User_List.size(); i++) {
				// if the other user is online, then broadcast him
				if ((!Username.equals(User_List.get(i).Name)) 
					&& User_List.get(i).LoggoutTime == 0) {
				ServerThread client = User_List.get(i).Thread;
				client.SendMessage(msg);
				}
			}
	    }

	    public void Broadcast(ArrayList<String> receivers, String msg) {
	    	for (int j = 0; j<receivers.size(); j++) {
	    		// search whethre the username is exist, if exist and online, message him.
	    		for (int i = 0; i<User_List.size(); i++) {
	    			if (receivers.get(j).equals(User_List.get(i).Name) 
	    				&& User_List.get(i).LoggoutTime == 0) {
	    				ServerThread client = User_List.get(i).Thread;
	    				client.SendMessage(msg);
	    				break;
	    			}
	    			if (i == User_List.size() - 1) {
	    				client_out.println("Receiver " + receivers.get(j) + " is not found or is offline.");
	    			}
				}
			}
	    }

        public void Message(String receiver, String msg) {
        	for (int i = 0; i<User_List.size(); i++) {
        		if (receiver.equals(User_List.get(i).Name)) {
        			if (User_List.get(i).LoggoutTime == 0) {
        				// if the message receiver is online, directly message him. 
        			    ServerThread client = User_List.get(i).Thread;
        			    client.SendMessage(msg);
        			    break;
        		    } else {
        		    	// if the message receiver has logged in but is offline him, message him later.
        		    	client_out.println("Receiver is not available now, sent him when available.");
        		    	User_List.get(i).OfflineMsg.add(msg);
        		    	break;
        		    }
        		}
        		if (i == User_List.size() - 1) {
        			// if the message receiver has never logged in, could not find him.
        		    client_out.println("Not such Receiver " + receiver + ".");
        		}
        	}
        } 

        public void OfflineMsg(ArrayList<String> msg) {
        	// to send offlinemessge to the client when he log in again.
        	for (int i = 0; i<msg.size(); i++) {
        		client_out.println(msg.get(i));
        	}
        }

        public void ResetPwd() {
        	try {
        		// to help the client reset his password.
        		file_in = new BufferedReader(new InputStreamReader(new FileInputStream("user_pass.txt")));
        	    client_out.println("Input the previous password again.");
        	    String line = client_in.readLine();
        	    String data[] = new String[2];
        	    String data_file = file_in.readLine();
                while (data_file != null) {
                	// get the user password of this user.
                    data = data_file.split(" ");
                    if (Username.equals(data[0])) {
            	        break;
                    }
            	    data_file = file_in.readLine();
                }
        	    if (!line.equals(data[1])) {
        	    	// check whethre the previous password is match.
        		    client_out.println("Password wrong, try again.");
        		    return;
        	    } else {
        		    client_out.println("Enter the new password");
        		    String line1 = client_in.readLine();
        		    client_out.println("Double Check the new password");
        		    String line2 = client_in.readLine();
        		    if (!line1.equals(line2)) {
        		    	// let the user type in new password twice 
        			    client_out.println("Double Check Failed, try again.");
        			    return;
        		    } else {
        		    	// begin to rewrite the user_pass.txt
        		    	file_in = new BufferedReader(new InputStreamReader(new FileInputStream("user_pass.txt")));
        		    	ArrayList<String> datastream = new ArrayList<String>();
        		    	String newdata_file = file_in.readLine();
        		    	while (newdata_file != null) {
        		    		// read in the whole file content
        			        if (newdata_file.equals(data_file)) {
        			        	newdata_file = newdata_file.replace(data[1], line2); 
        			        	// change the corrsponidng line
        			        	client_out.println("New combination: " + newdata_file);
        			        }
        			        datastream.add(newdata_file);
        			        newdata_file = file_in.readLine();
        			    }
        			    file_out = new PrintWriter(new FileOutputStream("user_pass.txt"));
        			    // rewrite the file content
        			    for (int i = 0; i< datastream.size(); i++) {
        			    	file_out.println(datastream.get(i));
        			    }
        		    }
        		    // close the file_in and file_out after reset
        		    file_in.close();
        		    file_out.close();
        	    }
        	} catch (Exception e) {
            }
        }

	    public void run() {
		    try{
		    	boolean authetication = false;
		        // to authetication the user
		        while (!authetication) {
		            authetication = Verification();
		        }
		        // after verification, close the file_in
		    	// the definition of new user.
		    	newUser.Name = Username;
		    	newUser.Thread = this;
		    	newUser.LoggoutTime = 0;
		    	newUser.OfflineMsg = new ArrayList<String>();
		    	VisitTimes ++;
		    	for (int i = 0; i<User_List.size(); i++) {
		    		// check whether user exist. If not new, then update it.
		    		if (User_List.get(i).Name.equals(Username)) {
		    			OfflineMsg(User_List.get(i).OfflineMsg);
		    			User_List.remove(i);
		    		}
		    	}
		    	User_List.add(newUser);
                Broadcast(Username + " come in the chat server.");
                // tell the online users that the user has logged in.
				Timer timer = new Timer();
				Task task = new Task();
				timer.schedule(task, 1000 * 60 * TIME_OUT);
			    String line = client_in.readLine();
			    // if the user not input anything in serveral mins, log it out; others reset the timer.
			    timer.cancel();
			    while (true) {
			    	if (line.equals("logout")) {
			    		// the logout command
			    		break;
			    	}
			    	else if (line.equals("reset password")) {
			    		// the reset command
			    		ResetPwd();
			    	}
				    else if (line.equals("whoelse")) {
				        // the whoelse command  
					    client_out.println(listOnlineUsers());
				    }
				    else if (line.equals("history")) {
				    	// the history command
				    	client_out.println(History());
				    }
				    else if (line.contains("wholast")) {
				    	// the wholast command.
				    	String[] cmdline = line.split(" ");
				    	//get the required time
				    	if (cmdline.length !=2) {
				    		client_out.println("Error, parameter wrong.");
				    	} else {
				    		int mins = Integer.parseInt(cmdline[1]);

				    	    client_out.println(listLastUsers(mins));
				    	}
				    }
				    else if (line.contains("broadcast user ")) {
				    	// broadcast user <user> <user> message <message> command
				    	String[] msgline = line.split(" ");
				    	// help to seperate the command itself, user and message.
				    	if (!msgline[0].equals("broadcast") || !msgline[1].equals("user")) {
				    		client_out.println("Error, failed to recognize command.");
				    	}
				    	else if (msgline.length <= 3) {
				    		client_out.println("Error, maybe forget the message or user.");
				    	} else {
				    		ArrayList<String> receivers = new ArrayList<String>();
				    		String s = Username + ": ";
				    		int index = 2; 
				    		for (; index < msgline.length; index++) {
				    			// add the receive user.
				    			if (msgline[index].equals("message")) {
				    				break;
				    			}
				    			receivers.add(msgline[index]);
				    		}
				    		if (index == msgline.length) {
				    			client_out.println("Error, cannot find the message index.");
				    		} else if (index == msgline.length -1) {
				    			client_out.println("Error, maybe forget the message.");
				    		}
				    		else { index ++;
				    		    for (; index < msgline.length; index++) {
				    			// add the message 
				    			    s += msgline[index] + " ";
				    		    }
				    		    Broadcast(receivers,s);
				    		    receivers.clear();
				    		}
				    	}
				    }
				    else if (line.contains("broadcast message ")) {
				    	// broadcast message <message> command
				    	String[] broadcastline = line.split(" ");
				    	// help to seperate the command itself and message.
				    	if (!broadcastline[0].equals("broadcast") || !broadcastline[1].equals("message")) {
				    		client_out.println("Error, failed to recognize command.");
				    	}
				    	else if (broadcastline.length <= 2) {
				    		client_out.println("Error, maybe forget the message.");
				    	} else {
				    		String s = Username + ": ";
				    		for (int i=2; i<broadcastline.length; i++) {
				    			s += broadcastline[i] + " ";
				    		}
				    		Broadcast(s);
				    	}
				    }
				    else if (line.contains("message ")) {
				    	// message <user> <message> command
				    	String[] messageline = line.split(" ");
				    	// help to seperate the command itself, user and message.
				    	if (!messageline[0].equals("message")) {
				    		client_out.println("Error, failed to recognize command.");
				    	}
				    	else if (messageline.length <= 2) {
				    		client_out.println("Error, maybe forget the message.");
				    	} else {
				    		String receiver = messageline[1];
				    		String s = Username + ": ";
				    		for (int i=2; i<messageline.length; i++) {
				    			s += messageline[i] + " ";
				    		}
				    		Message(receiver, s);
				    	}
				    }
				    else {
				    	client_out.println("Error, failed to recognize command.");
				    }
				    timer = new Timer();
				    task = new Task();
				    timer.schedule(task, 1000 * 60 * TIME_OUT);
				    // if the user not input anything in serveral mins, log it out; others reset the timer.
				    line = client_in.readLine();
				    timer.cancel();
			    }
			    Date time = new Date();
			    newUser.LoggoutTime = time.getTime();
			    // to get the loggout time of the user.
			    client_out.println("---See you, bye! ---");
			    client_out.println("logout");
			    client_out.flush();
			    client_out.close();
			    client_in.close();
			    Broadcast(Username + " left the chat server.");
			    // tell the online users that the user has logged out.
			    client.close();
		    } catch (IOException e) {
		    } finally {
			    try {
				    client.close();
			    } catch (IOException e) {}
		    }
	    }

	    public class Task extends TimerTask {
	    	// the timer is used to automatically shut down the client when time is out.
	    	public void run() {
	    		// to get the loggout time of the user.
	    		Date time = new Date();
			    newUser.LoggoutTime = time.getTime();
			    try {
			        client_out.println("Time is over!" + TIME_OUT + " mins.");
			        client_out.println("logout");
			        // tell the client socket to be closed.
			        client_out.close();
			        client_in.close();
			        Broadcast(Username + " left the chat server.");
			        // tell others the user is offline.
			        client.close();
			    } catch (IOException e) {
		        } finally {
			        try {
				        client.close();
			        } catch (IOException e) {}
		        }
	    	}
	    }

	    public String listOnlineUsers() {
	    	// to complete the whoelse function
		    String s = "Online list: \n";
		    for (int i = 0; i<User_List.size(); i++) {
		    	// if the user is online, add his name to the output line.
		    	if (! Username.equals(User_List.get(i).Name)) {
		    		if (User_List.get(i).LoggoutTime == 0) {
			            s += User_List.get(i).Name + " ";
			        }
			    }
		    }
		    return s;
	    }

	    public String History() {
	    	// to check the history users and visit times.
	    	String s = "Historic Users: ";
	    	s += String.valueOf(User_List.size());
	    	s += " Total VisitTimes: ";
	    	s += String.valueOf(VisitTimes);
	    	return s;
	    }

	    public String listLastUsers(int n) {
	    	// to complete the wholast function
	    	String s = "Last list: \n";
	    	Date time = new Date();
	    	long Current = time.getTime();
	    	// get the current time, to count the time difference
	    	for (int i = 0; i<User_List.size(); i++) {
	    		// if the user is online or loggout less than N mins, add his name to output line.
	    		if (! Username.equals(User_List.get(i).Name)) {
	    			if (User_List.get(i).LoggoutTime == 0 || 
	    				Current - User_List.get(i).LoggoutTime <= n*1000*60) {
	    				s += User_List.get(i).Name + " ";
	    			}
	    		}
	    	}
	    	return s;
	    }
    }

}
