a. The code contains two source files, the Server.java and the Client.java; 
   As named, the Client is utilized to receive the command from user, send it to the Server and receive corresponding response from Server and display it on the screen for the user. So obviously, the Client is just two thread, one to monitor the type in of user (the main thread), another to monitor the information sent be Server (the ReceiveMessage thread).
   And the Server is to handle the situations of multiple Clients. So it need several threads, one is to accept the new clients and distribute thread for them (the main thread), the others are utilized to receiver the command for clients, and operate these commands to send back corresponding results (the ServerTread threads). 
   First we need to verify the username and password (Autheticiation function), and as for each command, we have different functions, two for different broadcast commands (Broadcast functions, one for broadcast message command, another for broadcast user command), the message command (Message function), the whoelse command (listOnlineUsers function), the wholast command (listLastUsers function), the reset password command(ResetPwd function), the history command, (History function) and something to assist information propagation (SendMessage function). As I have added some other functions, part e would further illustrate.

b. java version "1.6.0_65"
   Java(TM) SE Runtime Environment (build 1.6.0_65-b14-468-11M4833)
   Java HotSpot(TM) 64-Bit Server VM (build 20.65-b04-468, mixed mode)

c. Firstly, you need to open the terminal and invoke both the Server and the Client, referring to the part d.
   The client would type in all commands in the Client terminal. You need to input the username and password, the Server would help you in that procedure, like telling you whether the username exist and whether your password is right. Once you successfully pass the verification, we would receive the hello from the server. The program would be more readable for new users, since I think the required one is too simple.

   Once the user log in, the other online users would receive the broadcast that the user is come in, as well when the user log out, they would broadcast the user left the chatroom. Then you could try several commands recommended in the assignment page, including "whoelse", "wholast int”, "broadcast message <message>", "message <user> <message>", "broadcast user <user> <user> message <message>", "history”, “reset password”, like that, even message the offline ones. When you type in "log out", the socket would be closed, connection is over. If you do not type in any commands for 30 mins, the Client would automatically log out as well.

   The invoke and quotation of each command above, except the two new ones I added, is defined by the assignment requirement. If your command could not be recognized, the server would tell you and the possible cause. The cause might not be so accurate, just as an reference.
   
   One words is banned in broadcast, that is logout. You could not “broadcast message logout” others, these would lead to mistakes, other words are all okay. And try not to include broadcast commands in the message command, like “message columbia broadcast user blabla” and “message wikipedia broadcast message hahaha”, the system could not recognize it. It is because of the priority order of semantics. In the program, the priority is broadcast message > broadcast user > message. The downer command should not include the upper ones.

   Also, one little bugs remain in the program, please try to avoid them. The first is the parameter of wholast command, don’t try to type in something like “wholast dasda”, make sure the parameter could be changed to integer, otherwise the program might break down.

   If you wanna close the Server, just use <ctrl+c> in the Server terminal, also Clients would receive a message said that the Server is closed and they would logout, then Server itself would be closed gracefully. And use <ctrl+c> to close the Client terminal would also be gracefully also, just like the logout command exit, not emphasising the routine work of other Clients and Server.
   
d. javac Server.java; java Server 4119; invoke the Server
   javac Client.java; java Client XXX.XXX.XXX.XXX(Current IP Address) 4119; invoke the Client
   the first two javac *.java command could be executed by the “make” command also.

e. I have added three extra functions. The first is offline message , the second user password reset, the three is the check the historic number of users.

   The first function is that when using the message <user> <message> command, if the receiver is offline, the message would be stored, and once the receiver log in again, he would automatically receive these messages, also the function could be adopted to broadcast command easily, but i don’t think it is essential.
   If wikipedia is offline, and facebook use "message wikipedia nice to meet you", then when wikipedia is log in again, he would receive "nice to meet you". 
   To be reminded, the offline user should have logged in the chat room sometime before. Although just change a little, to check whether the user existed in the user_pass.txt is also acceptable, but i think thus would more like how the reality server work, as new clients are gradually added in, but not a fixed size.

   The second function is that the user could have the access to change their password.
   For example, if wikipedia use the “reset password” command, the Server would have sequential procedures to help him confirm and alter to the new passwords. The user_pass.txt would be rewritten, so in the next login, wikipedia need to use the new password. 

   The third function is to use the history command to check the historical users, and their log in times, to know the digest rate.
   For example, if wikipedia have logged in twice, facebook have logged in three times, when facebook use the "history" command , he would receive "In total: 2 users, 5 times".

