import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.text.*;

public class receiver {
	public static short destPort;
	public static short ackPort;
	public static InetAddress source_IP;
	public static String logFile;
	public static String receiveFile;
	// the above variables referred to the assignment instruction
    public static Socket ackSocket;
    // the socket to send ACK to sender
    public static DatagramSocket packetSocket;
    // the UDP socket to receivce packet
    public static PrintWriter logWriter;
    public static final int MSS = 576;
    public static ArrayList<byte[]> packets;
    // the arraylist to buffer packets
    public static ArrayList<Integer> bufferNum;
    // the assist arraylist of the above list, to facilitate some operation

    public static void main(String[] args) {
    	// the main is to initial the variables
    	if (args.length != 5) {
    		System.out.println("Parameter Wrongness! Please refer to the Readme file.");
        	System.exit(1);
    	} else {
    		try {
    			receiveFile = args[0];
    			destPort = Short.parseShort(args[1]);
    			source_IP = InetAddress.getByName(args[2]);
    			ackPort = Short.parseShort(args[3]);
    			logFile = args[4];
    			File fileLog = new File(logFile);
    			packets = new ArrayList<byte[]>();
    			bufferNum = new ArrayList<Integer>();

        		if (logFile.equals("stdout")) {
                    logWriter = new PrintWriter(System.out, true);
        		} else {
                    logWriter = new PrintWriter(new FileOutputStream(logFile,true));
        		}
    			packetSocket = new DatagramSocket(destPort);
    			// the UDP socket set to the listening port
    		} catch (UnknownHostException e) {
                System.out.println(e.getLocalizedMessage());
                System.exit(1);
            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
                System.exit(1);
                // catch the Exceptions
            }
			runReceiver();
    	}
    }

    public static void runReceiver() {
    	int ackNum = 0;
    	int seqNum = 0;
    	FileOutputStream fileOutputStream;
    	/* to remind, what we retrieve from the packet start with large capital, like SeqNum
    	and what update by our receiver itself start with little capital, like seqNum */
    	try {
    		while (true) {
    			fileOutputStream = new FileOutputStream(receiveFile,true);
    			byte[] segment = new byte[MSS+20];
				DatagramPacket packet = new DatagramPacket(segment, segment.length);
				packetSocket.receive(packet);
				segment = packet.getData();
				// receivet the packet

				int sourcePort = packet.getPort();
                int SeqNum = writeLog(segment, sourcePort);
                // here also to get the SeqNum from packet 
                byte flag = segment[13];

                if (verification(segment)) {
                    if (flag == 17) {
                        for (int i = MSS + 19; i > 19; i--) {
                            if (segment[i] != 0) {
                                byte[] newsegment = new byte[i+1];
                                System.arraycopy(segment, 0, newsegment, 0, i+1);
                                segment = new byte[newsegment.length];
                                System.arraycopy(newsegment, 0, segment, 0, segment.length);
                            break;
                            }
                        }
                    }
                	// if it could pass the verification, if not, discard it
                	if (ackNum > SeqNum) {
                		// the duplicate packet
                		continue;
                	} else if (ackNum == SeqNum) {
                		// if receive the in need packet
                		byte[] data = new byte[segment.length - 20];
				        System.arraycopy(segment, 20, data, 0, data.length);
				        fileOutputStream.write(data);
				        // write it to the receive file
				        ackSocket = new Socket(source_IP, ackPort);
				        PrintWriter ackwriter = new PrintWriter(ackSocket.getOutputStream(), true);
				        ackwriter.println("SEQ " + SeqNum + " ACK " + ackNum + " FLAG " + flag);
				        writeAck(segment, SeqNum, ackNum);
				        // tell the sender successfully receive
				        ackNum ++;
				        // the in need packet number add by one
                        ackwriter.close();
                        // log the Ack information

                		while (bufferNum.contains(ackNum)) {
                			// find whether the packet in buffer is in need and write to the receive file
                            int index = bufferNum.indexOf(ackNum);
                			segment = packets.get(index);
                			flag = segment[13];
                		    data = new byte[segment.length - 20];
				            System.arraycopy(segment, 20, data, 0, data.length);
				            fileOutputStream.write(data);
				            bufferNum.remove(index);
				            packets.remove(index);
				            ackNum ++;
				        }
				        if (flag == 17) {
				        	// if the FIN is 17 and the packet has written to file, means the transmission is over
					        break;
				        }
                	} else if (ackNum < SeqNum) {
                		if (! bufferNum.contains(SeqNum)) {
                			// add the packet to buffer
                		    packets.add(segment);
                		    bufferNum.add(SeqNum);
                		}
                		ackSocket = new Socket(source_IP, ackPort);
                		// tell the sender successfully receive, but also need some previous packet
				        PrintWriter ackwriter = new PrintWriter(ackSocket.getOutputStream(), true);
				        ackwriter.println("SEQ " + SeqNum + " ACK " + ackNum + " FLAG " + flag);
				        ackwriter.close();
				        writeAck(segment, SeqNum, ackNum);
				        // log the ACK information
                	}
                }
			}
			packetSocket.close();
			logWriter.flush();
			fileOutputStream.close();
			System.out.println("Delivery completed successfully!");
			logWriter.close();
    	} catch (UnknownHostException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1);
		} catch (SocketException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1);
		}
    }

    public static void writeAck(byte[] segment, int SeqNum, int ackNum) {
    	// the same as the below writeLog, to update Ack information in log file
        byte flag = segment[13];
        try {
    	    String sourceAdd = InetAddress.getLocalHost().getHostAddress().toString() + ":" + ackSocket.getLocalPort();
    	    String destAdd = source_IP.toString().substring(1) + ":" + ackPort;
    	    Date date = new Date();
    	    SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	    String ack = "ACK: 1, ";
		    String fin = null;
		    if (flag == 17) {
			    fin = "FIN: 1";
		    } else {
			    fin = "FIN: 0";
		    }
		    logWriter.println("SendAck: " + date + ", Source: " + sourceAdd + ", Destination: " + destAdd + ", Seq Number: "
			+ SeqNum + ", Ack Number: " + ackNum + ", URG = 0, " + ack + "PSH = 0, RST = 0, SYN = 0, " + fin);
			// update the writelog about the ack information
	    } catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1);
		} 
    }

    public static int writeLog(byte[] segment, int sourcePort) {
    	// the writelog is used to update the logfile
    	byte[] temp = new byte[4];
		System.arraycopy(segment, 4, temp, 0, 4);
		int SeqNum = ByteBuffer.wrap(temp).getInt();
		temp = new byte[4];
		System.arraycopy(segment, 8, temp, 0, 4);
		int AckNum = ByteBuffer.wrap(temp).getInt();
		byte flag = segment[13];
		// retrieve the required information from the packet header
        try {
    	    String sourceAdd = source_IP.toString().substring(1) + ":" + sourcePort;
		    String destAdd = InetAddress.getLocalHost().getHostAddress().toString() + ":" + destPort;
		    Date date = new Date();
		    SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String ack = "ACK: 1, ";
		    String fin = null;
		    if (flag == 17) {
			    fin = "FIN: 1";
		    } else {
			    fin = "FIN: 0";
		    }

            logWriter.println("Receive: " + date + ", Source: " + sourceAdd + ", Destination: " + destAdd + ", Seq Number: "
			+ SeqNum + ", Ack Number: " + AckNum + ", URG = 0, " + ack + "PSH = 0, RST = 0, SYN = 0, " + fin);
			// update the writelog
	    } catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1);
		} 
		return SeqNum;
    }

	public static boolean verification(byte[] segment) {
		// to verify the checksum
		byte[] temp = new byte[2];
		System.arraycopy(segment, 0, temp, 0, 2);
		short AckPort = ByteBuffer.wrap(temp).getShort();
        temp = new byte[2];
		System.arraycopy(segment, 2, temp, 0, 2);
		short DestPort = ByteBuffer.wrap(temp).getShort();
        byte flag = segment[13];
		// retrevet the AckPort and DestPort
		short checksum = (short) (AckPort + DestPort + flag);  
        temp = new byte[2];
        System.arraycopy(segment, 16, temp, 0, 2);
        short checkSum = ByteBuffer.wrap(temp).getShort();
        // get the original checksum
		for (int i=20; i < MSS+20; i=i+2) {
			if (i == segment.length - 1) {
        		checksum += segment[i] * 16;
        	} else {
        		checksum += segment[i] * 16 + segment[i+1];
        	}
		}
		// use the information to recount the checksum
        short inverse = (short) ~checksum;
		if (checkSum == inverse) {
			// compare the two results
			return true;
		} else {
			return false;
		}
	}

}
