import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.text.*;

public class sender {
	public static ArrayList<byte[]> packets;
	// the arraylist of packets
	public static ArrayList<Integer> indexList;
	// the seq num of packets, could be omitted, here used to facilitate some operations
	public static ArrayList<Long> startTime;
	// the startTime of each packet, to count the RTT accurately
	public static String sendFile;
	public static String logFile;
	public static short ackPort;
	public static short remotePort;
	public static short windowSize;
	public static long estimateRTT;
	public static long devRTT;
	public static long timeoutTime;
	public static InetAddress dest_IP;
	// the above variables referred to the assignment instruction
	public static DatagramSocket packetSocket;
	// the UDP packet transmission socket
	public static ServerSocket serverSocket;
	// the ack listening socket
	public static int totalRetransmissions;
	public static int totalSegments;
	public static int totalBytes;
	public static PrintWriter logWriter;
	public static final int MSS = 576;
	// the MSS set as 576, it could be changed

	public static void main(String[] args) {
		// the main function is to intialize all the variables
        if (args.length !=5 && args.length != 6) {
        	System.out.println("Parameter Wrongness! Please refer to the Readme file.");
        	System.exit(1);
        } else {
        	try {
        		sendFile = args[0];
        		dest_IP = InetAddress.getByName(args[1]);
        		remotePort = Short.parseShort(args[2]);
        		ackPort = Short.parseShort(args[3]);
        		logFile = args[4];
        		File fileLog = new File(logFile);
        		if (logFile.equals("stdout")) {
                    logWriter = new PrintWriter(System.out, true);
        		} else {
                    logWriter = new PrintWriter(new FileOutputStream(logFile,true));
        		}
        		if (args.length == 6) {
        			windowSize = Short.parseShort(args[5]);
        		} else {
        			windowSize = 1;
        		}
        		packetSocket = new DatagramSocket();
        		// the UDP packet port numbet is not defined in the invoke line
        		serverSocket = new ServerSocket(ackPort);
        		estimateRTT = 0;
        		devRTT = 0;
        		timeoutTime = 1000;
        	} catch (IOException e) {
        		System.out.println(e.getLocalizedMessage());
        		System.exit(1);
        	}
        	runSender();
        }
	}

	public static void runSender() {
		// the runSender is to implement the sender main function
		int windowStart = 0;
		int windowEnd = 0;
		int ackCount = 0;
		int ackLast = 0;
		// for fast retransmission 
		try {
			generatePacket();
			// generate the TCP packets
			int termite = packets.size();
			long[] startTime = new long[termite];
			// use to store the startime of each packet, to count the RTT
			while (packets.size() != 0) {
				if (windowEnd < windowStart + windowSize && windowEnd != termite) {
					// determine whether to send packets or listen to acks
					if (indexList.contains(windowEnd)) {
						//if the packet have sent successfully, do not need to resend
                        int index = indexList.indexOf(windowEnd);
				        byte[] segment = packets.get(index);
				        DatagramPacket packet = new DatagramPacket(segment, segment.length, dest_IP, remotePort);
				        packetSocket.send(packet);
				        // configute the iP and port, send the packet
				        startTime[windowEnd] = System.currentTimeMillis();
				        totalSegments ++;
				        totalBytes += segment.length;
				        writeLog(segment);
				        // write the log
				    }
				    windowEnd ++;
				} else {
					serverSocket.setSoTimeout((int) timeoutTime);
					// set the timeout for serversocket and acksocket
					Socket ackSocket = null;
                    String response = null;
                    try {
					    ackSocket = serverSocket.accept();
					    ackSocket.setSoTimeout((int) timeoutTime);
					    BufferedReader ackReader = new BufferedReader(new InputStreamReader(ackSocket.getInputStream()));
					    response = ackReader.readLine();
					    // accept the socket and read the ack
					} catch (SocketTimeoutException e) {
						// if no ack receive for some time, means the packet is lost or corrupted
						if (windowEnd < termite ) {
					        totalRetransmissions += indexList.indexOf(windowEnd);
					    } else {
					    	totalRetransmissions += packets.size();
					    }
					    // count the number of packets need to resend
					    windowEnd = windowStart;
					    // reset the windowstart to resend
					    continue;
					}

					String temp = response.split(" ")[1];
					int seqNum = Integer.parseInt(temp);
					calculateTimeout(startTime[seqNum]);
					temp = response.split(" ")[3];
					int ackNum = Integer.parseInt(temp);
					// split the ack to get useful information
					int sourceport = ackSocket.getPort();
					writeAck(response, sourceport);
					// update the ack information
					windowStart = ackNum;

                    /*  the fast transmission
                    if (ackNum < seqNum) {
                    	if (ackNum == ackLast) {
                            ackCount ++;
                            if (ackCount == windowSize / 3) {
                            	int index = indexList.indexOf(windowStart);
				                byte[] segment = packets.get(index);
				                DatagramPacket packet = new DatagramPacket(segment, segment.length, dest_IP, remotePort);
				                packetSocket.send(packet);
				                startTime[ackNum] = System.currentTimeMillis();
				                totalRetransmissions ++;
				                totalSegments ++;
				                totalBytes += segment.length;
				                writeLog(segment);
				                ackCount = 0;
                            }
                    	} else {
                            ackCount = 0;
                            ackLast = ackNum;
                    	}
                    } */

					if (indexList.contains(seqNum)) {
						// the packet is sent successfully, so remove it from the arraylist
				    	int index = indexList.indexOf(seqNum);
				    	indexList.remove(index);
				    	packets.remove(index);
				    }
					// the calculate might not be so accurate, but enough the encounter delay
					while ((!indexList.contains(windowStart)) && windowStart != termite)  {
						//find whether the following packet have sent successfully
					    windowStart ++;
					}
				    ackSocket.close();
				    // close the time countdown
				}
			}
			serverSocket.close();
			logWriter.flush();
			System.out.println("Delivery Completed Successfully!");
		    System.out.println("Total bytes sent = " + totalBytes);
		    System.out.println("Segments sent = " + totalSegments);
		    System.out.println("Segments retransmissed = " + totalRetransmissions);
		    // output what the instruction need, three indexes
		    logWriter.close();
		} catch (UnknownHostException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1);
			// catch the exceptions and output them
		}
	}

	public static void generatePacket() {
		// the generatePacket generate TCP packet(segment)
		ArrayList<byte[]> datum;
		// the arraylist of data part
		ArrayList<byte[]> headers;
		// the arraylist of header part
		int ackNum = 0;
		int seqNum = 0;
		// here in the segment, the ackNum and seqNum are identical
        
        datum = new ArrayList<byte[]>();
        try {
		    File file = new File(sendFile);
		    byte[] fileData = new byte[(int) file.length()];
		    FileInputStream fileReadStream = new FileInputStream(file);
		    // get all the data in the file to the fileData array
		    fileReadStream.read(fileData);
		    fileReadStream.close();
		    byte[] instant = new byte[MSS];
		    int count = fileData.length / MSS;
		    for (int i=0; i<fileData.length; i++) {
			    // cut the fileData to length of MSS and save to the list of data
			    if (i != 0 && i % MSS == 0) {
                    datum.add(instant);
                    instant = new byte[MSS];
			    }
			    instant[i % MSS] = fileData[i];
		    }
		    int overcount = fileData.length % MSS;
		    // handle the last remaining bytes
		    byte[] overflow = new byte[overcount];
		    System.arraycopy(fileData, count * MSS, overflow, 0, overcount);
		    datum.add(overflow);
		} catch (IOException e) {
			    System.out.println(e.getLocalizedMessage());
			    System.exit(1);
		}

        headers = new ArrayList<byte[]>();
        // to construct the correspoding header
		for (int i=0; i<datum.size(); i++) {
			// the following procedure fill in all the header except the checksum
			byte[] tcpHeader = new byte[20];
			ByteBuffer buffer = ByteBuffer.allocate(2);
			byte[] temp = buffer.putShort(ackPort).array();
			System.arraycopy(temp, 0 , tcpHeader, 0, 2);
			buffer = ByteBuffer.allocate(2);
			temp = buffer.putShort(remotePort).array();
			System.arraycopy(temp, 0 , tcpHeader, 2, 2);
			buffer = ByteBuffer.allocate(4);
			temp = buffer.putInt(seqNum).array();
			System.arraycopy(temp, 0 , tcpHeader, 4, 4);
			buffer = ByteBuffer.allocate(4);
			temp = buffer.putInt(ackNum).array();
			System.arraycopy(temp, 0 , tcpHeader, 8, 4);
			tcpHeader[12] = 80;
			if (i == datum.size() - 1) {
				// the FIN flag
				tcpHeader[13] = 17;
			} else {
				tcpHeader[13] = 16;
			}
			buffer = ByteBuffer.allocate(2);
			temp = buffer.putShort(windowSize).array();
			System.arraycopy(temp, 0 , tcpHeader, 14, 2);
			tcpHeader[18] = 0;
			tcpHeader[19] = 0;
			headers.add(tcpHeader);
			seqNum ++;
			ackNum ++;
			// the sequence need to increment by 1
		}

        packets = new ArrayList<byte[]>();
        // combine the header and data to a packet
        indexList = new ArrayList<Integer>();
        // the assist arraylist of packet
        for (int i=0; i<datum.size(); i++) {
        	byte[] header = headers.get(i);
        	byte[] data = datum.get(i);
        	byte[] tcppacket = new byte[header.length + data.length];
        	byte flag = header[13];
        	short checksum = (short) (ackPort + remotePort + flag);
        	// calculate the checksum, using port number and all the datum
        	for (int j=0; j<data.length; j=j+2) {
        		if (j == data.length - 1) {
        			checksum += data[j] * 16;
        		} else {
        			checksum += data[j] * 16 + data[j+1];
        		}
        	}
        	short inverse = (short) ~checksum;
            ByteBuffer buffer = ByteBuffer.allocate(2);
			byte[] temp = buffer.putShort(inverse).array();
        	System.arraycopy(temp, 0, header, 16, 2);
        	// put the checksum to the corresponding place
        	System.arraycopy(header, 0, tcppacket, 0, header.length);
        	System.arraycopy(data, 0, tcppacket, header.length, data.length);
        	// combine the header and data
        	packets.add(tcppacket);
        	indexList.add(i);
        }
	}

	public static void writeAck(String response, int sourceport) {
        String temp = response.split(" ")[1];
	    int seqNum = Integer.parseInt(temp);
	    temp = response.split(" ")[3];
		int ackNum = Integer.parseInt(temp);
		temp = response.split(" ")[5];
		int flag = Integer.parseInt(temp);

        try {
    	    String sourceAdd = dest_IP.toString().substring(1) + ":" + sourceport;
    	    String destAdd = InetAddress.getLocalHost().getHostAddress().toString() + ":" + ackPort;
    	    Date date = new Date();
    	    SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	    String ack = "ACK: 1, ";
		    String fin = null;
		    if (flag == 17) {
			    fin = "FIN: 1";
		    } else {
			    fin = "FIN: 0";
		    }
		    logWriter.println("ReceiveAck: " + date + ", Source: " + sourceAdd + ", Destination: " + destAdd + ", Seq Number: "
			+ seqNum + ", Ack Number: " + ackNum + ", URG = 0, " + ack + "PSH = 0, RST = 0, SYN = 0, " + fin + ", Estimated RTT: " + estimateRTT);
			// update the writelog about the ack information
	    } catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1);
		}         
	}

	public static void writeLog(byte[] segment) {
		// the writelog is to upadte the logfile
		byte[] temp = new byte[4];
		System.arraycopy(segment, 4, temp, 0, 4);
		int seqNum = ByteBuffer.wrap(temp).getInt();
		temp = new byte[4];
		System.arraycopy(segment, 8, temp, 0, 4);
		int ackNum = ByteBuffer.wrap(temp).getInt();
		byte flag = segment[13];
		String ack = "ACK: 1, ";
		String fin = null;
		if (flag == 17) {
			fin = "FIN: 1";
		} else {
			fin = "FIN: 0";
		}
		// retrieve the required paremeters

        try {
		    String sourceAdd = InetAddress.getLocalHost().getHostAddress().toString() + ":" + packetSocket.getLocalPort();
		    // get the sourceadd of UDP packet socket
		    String destAdd = dest_IP.toString().substring(1) + ":" + remotePort;
		    Date date = new Date();
		    SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    // get the time

		    logWriter.println("Send: " + date + ", Source: " + sourceAdd + ", Destination: " + destAdd + ", Seq Number: "
			    + seqNum + ", Ack Number: " + ackNum + ", URG = 0, " + ack + "PSH = 0, RST = 0, SYN = 0, " + fin + ", Estimated RTT: " + estimateRTT);
		    //write all the things to the logfile
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1);
		}
	}

	public static void calculateTimeout(long startTime) {
		// the caluculateTimeout calculate the changable timeout time
		long endTime = System.currentTimeMillis();
		long sampleRTT = endTime - startTime;
		estimateRTT = new Double(0.875 * estimateRTT + 0.125 * sampleRTT).longValue();
		devRTT = new Double(0.75 * devRTT + 0.25 * Math.abs(sampleRTT - estimateRTT)).longValue();
		timeoutTime = new Double(estimateRTT + 4 * devRTT).longValue();
		// the calculate of timeout referred to the textbook
		if (timeoutTime < 10) {
			timeoutTime = 10;
		}
		/* sometimes the calculated timeoutTime is too small, and would lead to some interesting problems,  
		so here to set a threshold*/
	}
}
