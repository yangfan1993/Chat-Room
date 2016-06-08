(a) TCP segment structure:
    The TCP segment structure is referred to the Figure 3.29 of the Computer Networking A Top-down Approach 6th Edition. It could be divided into two parts, the data part and the header part. The data part has the length MSS (variable, in my program original set as 576), excepts the last segment. It directly extract the bytes from the send file.
    The second in is the header, it has 20 bytes. The first two is source port (ack port in the sender source code), the next two is dest port, (remote port in the sender source code). The next four is the Seq Number, which indicates the sequence of segments. The next four is Acknowledgement Number, in the true TCP it would be different from the Sequence Number, but here they are initially all 0, so they would be the same. The next two is flags, but we just need to consider the ACK and FIN bit in it. The header length is 20, so the two bytes would be 80H, 16H or 80H 16H (FIN = 0), 80H 17H (FIN=1). The next two is the receive window length, it is used for flow control, we could disregard it or set as the window size. The next two is the checksum, here we add all the datum bytes and the source port, dest port, flag to count the checksum. I have not included the seq num and ack num, because these error might be detected in other procedures, and the remaining bytes are mostly useless. The last two, urgent data pointers, are zero.

(b) The working state of receiver: 
    1. receive the packet, could not pass checksum verification. Then the packet is corrupted, simply discard it is ok, the sender would resend it.
    2. receive the packer, pass the checksum. The Sequence number of the packets perfectly match the current waiting packet, write it to the receive file directly, and check the packet buffer to search whether have following packet to write to the receive file. And send ACK to sender you have received the packet. 
    3. receive the packer, pass the checksum. The Sequence number of the packets less than the current waiting one, this must be the duplicated one, discard it.
    4. receive the packer, pass the checksum. The Sequence number of the packets larger than the current waiting one, put it to the packet buffer, and send ACK to sender to tell you have received the packet and indicate your currently waiting packet sequence number.
    
    The initialing state of sender: 
    Extract all the datum in the sending file, and utilize thus to construct the TCP segments (packets). The construct of headers could referred to the part(a).

    The working state of sender:
    1. the windowend - windowstart < windowsize, then we need to send the packets, to maintain the fixed windowsize, which means that the maximum sequence difference of packets we keep track of is always the windowsize, except for the few last packets. Thus we could maintain the pipelining send of packets
    2. the windowend - windowstart = windowsize, then to wait for ACK. The Sequence number of ACK match the current windowstart, then the delete the corresponding packet from the sending list, and increment the windowstart by 1, meaning the packet is sent successfully, and check whether latter packets have sent, if sent, still increment the windowstart.
    3. the windowend - windowstart = windowsize, then to wait for ACK. The Sequence number of ACK larger than the current windowstart, then the delete the corresponding packet from the sending list, the packet have sent successfully, but some of its predecessor encountered lossy, so we could not increment windowstart.
    4. the timeout happens, which means some of the packets in the windowsize that has been lost in the UDP channel or corruputed, we need to resend them, so we resend all the packets in windowsize that have not yet received corresponding ACK.

    Still, we could add something in the first three states to implement the fast retransmission, which is used to accelerate the timeout procedure. But here based on the above implementation, it is enough for us to achieve reliable transmission, the new mechanism would only add the burden and emphasize little performance, since the situation simulated by proxy is not that kind of chaos. In the sender.java, I have not deleted it and set it as comments.

    At first, I thought that as for the sender, I need two threads to work simultaneously, one the send packet and another receiving the ACKs so as to maintain the state. However, after test I found that I does not need, just to send packets or receive ACKs at one time is also workable. So the program might not be robust enough, but so far I have found nothing wrongness.

(c) Here we assume about the packet would be lossy, the transmission of ACKs are always reliable.
    Lossy recovery mechanism:
    As the (b) part have illustrated part. There are five kinds of lossy situations mentioned in instruction.
    As for packet loss, the timeout mechanism of sender would resend the file. If the receiver receive the following packet first, it would put it in the buffer.
    As for corrupted packet, use the checksum to detect it, and discard it, the timeout mechanism of sender would resend the file.
    As for packet delays, the calculate timeout time mechanism would change the timeout time, to adapt to the current delay circumstance.
    As for duplicate packet, the receiver would compare it would the current waiting Seq number to determine whether it is duplicate, and discard if it is.
    As for packets out of order, it is the same as the packet loss.

(d) The invoke of sender:
    java sender <filename> <remote_IP> <remote_port> <ack_port_num> <log_filename> <window_size>
    The invoke of receiver:
    java receiver <filename> <listening_port> <sender_IP> <sender_port> <log_filename>
    A traditional invoke of sender, receiver and proxy is :
    java sender new.txt 209.2.224.153 4003 4001 stdout 20
    java receiver test.txt 4000 209.2.224.153 4001 stdout
    ./newudpl -p 4003:4002 -i 209.2.224.153:* -o 209.2.224.153:4000 -L 30 -B 1000 -O 30 -d 0.1 
    You can use my new.txt file for test of sending.

(e) Tips:
    Always keep your invoke instruct correctly. Avoid trying to use the too large window_size, around 10 would be appropriate, if it is larger than 20, it would have difficulty in maintaining it, although the output is right, sometimes even in the simple situation, without lossy, it would happen retransmit. 
    And try not use the too long document the total sent bytes under 500000 bytes is perfect, as the number grows, more than 1000000 bytes, i have once meet that some of the last information loss, maybe it is because the information loss due to too long ArrayList, but try avoid thus. 
    Avoid testing in the too complex circumstance, I mean 80 percent packets loss or something like that. The simple TCP is so far robust, but I don’t know what would happen under thus situations.
    The RTT’s unit in the log file is milliseconds, as default in most of the computer configuration, and when i set the delay as 0.1 second, the RTT is always around 120, thus i should be millisecond.
    The last but most important thing, try not to include NULL, the eight byte zero in the very last of your sending file (i mean the last packet of your file). Obviously, it is not traditionally used in our files. Because in the checksum we have not encapsulation its length, so we could only use the null to check whether it reaches its end. If we do not detect its last byte, it would have somewhat several NULL remaining in the last, which would not be recognized by the Mac TextEdit but could be recognized by the Sublime Text.
