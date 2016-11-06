/* 2016-10-30
-TO EXECUTE: 
1. Start the HostServer in some command prompt. >> java HostServer

2. start a web browser and navigate to http://localhost:1565. Enter some text in the text field provided
 and hit the submit button to start a state-maintained conversation.

3. start a second web browser and navigate to http://localhost:1565 and do the same.
This will start at a different port number when compared to the initial browser

4. To command the agent to change port, enter the string "migrate"
in the text box and submit. The agent will migrate to a new port, but keep its old state.

During migration, stop at each step and view the source of the web page to see how the
server informs the client where it will be going in this stateless environment.

-----------------------------------------------------------------------------------

DESIGN OVERVIEW

Here is the design overview of what each class does:

HOST SERVER
  Runs on some machine and listens for requests made at port 1565
  The clients will be assigned a port which is greater than 3000
  Each new request from clients will be assigned to a port greter than 3000 and will be incremented by 1 till the next available port
  
AGENT LOOPER/LISTENER
  Creates an initial state or maintains an existing state of this client has opted for 'migrate'
  Then this listener gets an available port form the hostserver and maintains the previous state that it received
  It then sends the new port number to the client browser
  After this listener has accepted a request it call the agent worker which processes the further requests from the client
  
  
AGENT WORKER
  This worker thread just updates the current state.
  If the user(clients on web browser) decides to migrate then this worker will send a request for hosting at a new port
  With this request the current state is also appended i order to serve from the current state at the new port.
  It also sends a HTML form to the user which opoints to the new port and also the current state for the user
  
WEB CLIENT
  Just a standard web browser pointing to http://localhost:1565 to start.

-----------------------------------------------------------------------------------  
COMMENTS:

This program listens for requests at the port 1565 by default
and each client is allocated a port number greater than 3000
This program is used to migrate agents form one port ot the other
without forgetting the state of the converstion they had in the previous port.
The program can listen to various clients and proces their requests from multile ports.

State is maintained by an integer which is do=isplayed each the HTML form is sent to the client's browser


  -------------------------------------------------------------------------------*/

/*import all the required libraries*/
import java.io.BufferedReader;	
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

 /* AgentWorker objects are created by agenlisteners in order ot process the requests made at the ports which are listening for clients. If this class encounters 'migrate' as
  * an inout string it will migrate to the next  available port which is greater than 3000. */
 
class AgentWorker extends Thread {	
	
	Socket sock; //connection to client
	agentHolder parentAgentHolder; //maintains agentstate holding socket and state counter
	int localPort; //port being used by this request
	
	//basic constructor
	AgentWorker (Socket s, int prt, agentHolder ah) {
		sock = s;	/*socket variable*/
		localPort = prt;	/*port number will be stored in this variable*/
		parentAgentHolder = ah;/*holds the state of the current client running on this port*/
	}
	public void run() {
		
		//initialize variables
		PrintStream out = null;	/*output stream*/
		BufferedReader in = null;/*input stream*/
		//server is hardcoded in, only acceptable for this basic implementation
		String NewHost = "localhost"; /*serverName*/
		
		int NewHostMainPort = 1565;		/*Port at which the main worker will accept clients.*/
		String buf = "";
		int newPort;
		Socket clientSock;
		BufferedReader fromHostServer;
		PrintStream toHostServer;
		
		try {
			out = new PrintStream(sock.getOutputStream());
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			String inLine = in.readLine();	/*The input line that we get from the client*/
			StringBuilder htmlString = new StringBuilder();	/*StringBuilder is used in orderr tp accept data of any type*/
			System.out.println();
			System.out.println("Request line: " + inLine);	/*log/display the request on the shell*/
			
			if(inLine.indexOf("migrate") > -1) {
				//if the client wants the agent to migrate to a new port then he'll type migrate
				
				//create a new socket with the main server waiting on 1565
				clientSock = new Socket(NewHost, NewHostMainPort);
				fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
				//send a request to port 1565 to receive the next open port
				toHostServer = new PrintStream(clientSock.getOutputStream());
				toHostServer.println("Please host me. Send my port! [State=" + parentAgentHolder.agentState + "]");
				toHostServer.flush();
				
				//wait for the response and read a response until we find what should be a port
				for(;;) {
					//read the line and check it for what looks to be a valid port
					buf = fromHostServer.readLine();
					if(buf.indexOf("[Port=") > -1) {
						break;
					}
				}
				
				//get the new port from the port response.
				String tempbuf = buf.substring( buf.indexOf("[Port=")+6, buf.indexOf("]", buf.indexOf("[Port=")) );
				//from the port response, we are extracting the port number which will be in character format and hence we are using the parseInt to convert the string port number to integer port number
				newPort = Integer.parseInt(tempbuf);
				//log it to the server console or shell 
				System.out.println("newPort is: " + newPort);
				
				//create the html response which will be sent to the client's browser
				htmlString.append(AgentListener.sendHTMLheader(newPort, NewHost, inLine));
				//inform the user the migration request has been received and proessed
				htmlString.append("<h3>We are migrating to host " + newPort + "</h3> \n");
				htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n");
				//send the html response to the user
				htmlString.append(AgentListener.sendHTMLsubmit());

				//We are killing the waiting server at the port msg would be displayed on the shell in which the hostserver is being executed
				System.out.println("Killing parent listening loop.");
				
				ServerSocket ss = parentAgentHolder.sock;
				//get the old port number and close the port in order to prevernt resource leaks
				ss.close();
				
				
			} else if(inLine.indexOf("person") > -1) {
				//increment the state int to reflect an event occuring in the 'game'
				parentAgentHolder.agentState++;
				//send the html back to the user displaying the agent current state and the associated html form
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
				htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n");
				htmlString.append(AgentListener.sendHTMLsubmit());

			} else {
				//we couldnt find a person variable, so we probably are looking at a fav.ico request
				//tell the user it was invalid
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
				htmlString.append("You have not entered a valid request!\n");
				htmlString.append(AgentListener.sendHTMLsubmit());		
				
		
			}
			//output the html
			AgentListener.sendHTMLtoStream(htmlString.toString(), out);
			
			//close the socket
			sock.close();
			
			
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
	
}

class agentHolder {	/*Holds the state info so that it can used to tack the state while changing the ports*/
	//active serversocket object
	ServerSocket sock;
	//basic agentState var
	int agentState;
	
	//basic constructor
	agentHolder(ServerSocket s) { sock = s;}
}

class AgentListener extends Thread {	/*Will listen at the ports for requests from the clients browser. This will instantiate a new object when a new request is made to the server at port 1565*/
	//instance variables
	Socket sock;
	int localPort;
	
	//basic constructor to initialise the agentListener object
	AgentListener(Socket As, int prt) {
		sock = As;
		localPort = prt;
	}
	//by default the agent state is set to 0
	int agentState = 0;
	
	//called from start() when a request is made on the listening port
	public void run() {
		BufferedReader in = null;	/*input stream*/
		PrintStream out = null;	/*output stream*/
		String NewHost = "localhost";	/*program will be running on localserver*/
		System.out.println("In AgentListener Thread");		
		try {
			String buf;
			out = new PrintStream(sock.getOutputStream());
			in =  new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			//read first line from the input stream
			buf = in.readLine();
			
			//if we have a state, parse the request and store it
			if(buf != null && buf.indexOf("[State=") > -1) {
				//extract the state from the read line
				String tempbuf = buf.substring(buf.indexOf("[State=")+7, buf.indexOf("]", buf.indexOf("[State=")));
				//parse it to the integer format
				agentState = Integer.parseInt(tempbuf);
				//dispplay it on the shell in which the program is being executed
				System.out.println("agentState is: " + agentState);
					
			}
			
			System.out.println(buf);	/*display the input line received*/
			StringBuilder htmlResponse = new StringBuilder();	//string builder to hold the html response
			//output first request html to user
			//show the port and display the form. we know agentstate is 0 since game hasnt been started
			htmlResponse.append(sendHTMLheader(localPort, NewHost, buf));
			htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n");
			htmlResponse.append("[Port="+localPort+"]<br/>\n");
			htmlResponse.append(sendHTMLsubmit());
			//send the html response using the output stream
			sendHTMLtoStream(htmlResponse.toString(), out);
			
			
			ServerSocket servsock = new ServerSocket(localPort,2);
			//create a new socket and also the agentholder to store the socket and agentState
			agentHolder agenthold = new agentHolder(servsock);
			agenthold.agentState = agentState;
			
			//listen and accept the connections that come to the port
			while(true) {
				sock = servsock.accept();
				//display the received connection
				System.out.println("Got a connection to agent at port " + localPort);
				//after the connection has been displayed create an agentWorker to handle this client
				new AgentWorker(sock, localPort, agenthold).start();
			}
		
		} catch(IOException ioe) {
			//to handle any exception which arises due to closure of the port
			System.out.println("Either connection failed, or just killed listener loop for agent at port " + localPort);
			System.out.println(ioe);
		}
	}
	/*send the html header, this will create a html form which will ask the user to input a text. This is different from the HTML response header.
	This will also show the current port number and also the previous text that the user inserted in the previous form and also the current state*/
	static String sendHTMLheader(int localPort, String NewHost, String inLine) {
		
		StringBuilder htmlString = new StringBuilder();

		htmlString.append("<html><head> </head><body>\n");
		htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + NewHost + "</h2>\n");
		htmlString.append("<h3>You sent: "+ inLine + "</h3>");
		htmlString.append("\n<form method=\"GET\" action=\"http://" + NewHost +":" + localPort + "\">\n");
		htmlString.append("Enter text or <i>migrate</i>:");
		htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n");
		
		return htmlString.toString();
	}
	//send the html form created by the sendHTMLheader function
	static String sendHTMLsubmit() {
		return "<input type=\"submit\" value=\"Submit\"" + "</p>\n</form></body></html>\n";
	}
	//send the response headers which include content length and type as done in previous assignments
	static void sendHTMLtoStream(String html, PrintStream out) {
		
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + html.length());
		out.println("Content-Type: text/html");
		out.println("");		
		out.println(html);
	}
	
}


/*main class which will listen on port 1565 for new requests. After recieving a request the nextPort will be
* incremented by 1 and the ports for this will start at 3000*/

public class HostServer {	
	//we start listening on port 3001
	public static int NextPort = 3000;
	
	public static void main(String[] a) throws IOException {
		int q_len = 6;
		int port = 1565;
		Socket sock;
		
		ServerSocket servsock = new ServerSocket(port, q_len);
		System.out.println("Sasi's DIA Master receiver started at port 1565.");
		System.out.println("Connect from 1 to 3 browsers using \"http:\\\\localhost:1565\"\n");
		//listen on port 1565 for new requests OR migrate requests
		while(true) {
			//increment nextport! could be more sophisticated, but this will work for now 
			NextPort = NextPort + 1;
			//open socket for requests
			sock = servsock.accept();
			//log startup
			System.out.println("Starting AgentListener at port " + NextPort);
			//create new agent listener at this port to wait for requests
			new AgentListener(sock, NextPort).start();
		}
		
	}
}