/*
Program: BCHandler
By: Sasidhar Mukthinuthalapati
Date: 10/23/2016

This is the BCHandler program will handle the .xyz file and also marshall data suing the back channel to the MyWebServer program. This will send 7 lines of data to the MyWebServer program.


java version "1.8.0_101"
Java(TM) SE Runtime Environment (build 1.8.0_101-b13)
Java HotSpot(TM) Client VM (build 25.101-b13, mixed mode)


To Compile this program type the following:
javac -cp "C:\Program Files (x86)\Java\jre1.8.0_101\lib\ext\xstream-1.2.1.jar;C:\Program Files (x86)\Java\jre1.8.0_101\lib\ext\xpp3_min-1.1.3.4.O.jar;" BCHandler.java

To Run this program type the following:
java BCHandler


The files which you need with this are:
BCClient.java
Handler.java
shim.bat
mimer-discussion.html
mimer-call.html
checklist-mimer.html
Server-log.txt contains the outputs of the whole process.

I would suggest using Internet Explorer if possible.


----------------------------------------------------------------------*/






import java.io.*;  // Get the Input Output libraries
import java.net.*; // Get the Java networking libraries
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

class myDataArray {
  int num_lines = 0;
  String[] lines = new String[8];
}

public class BCHandler{
  private static String XMLfileName = "C:\\temp\\mimer.output";/*The temporary folder in which the mimer.output will contain the data marshalled in xml format.*/
  private static PrintWriter      toXmlOutputFile;	/*output stream to write into the xml file*/
  private static File             xmlFile;
  
  public static void main (String args[]) {
    String serverName;
    String argOne = "WillBeFileName"; 
    if (args.length < 1) serverName = "localhost";
    else serverName = args[0];
    XStream xstream = new XStream();/*We will use the Xstream to flatten the data object into the XML format*/
    String[] testLines = new String[4];  int i=0;
    myDataArray da = new myDataArray();	/*data array to store the data which will be marshalled to the MyWebServer program*/
    System.out.println("Sasi's back channel & handler is being executed.\n");
    System.out.println("Using server: " + serverName + ", Port: 2570");
	try{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); /*input stream reader*/
		while(((da.lines[i++] = in.readLine())!= null ) && i<8) /*will read only 7 lines*/
		{System.out.println("data is:"+ da.lines[i-1]);
		}
		da.num_lines = i-1;
		System.out.println("Number of lines is:"+da.num_lines);
		String xml = xstream.toXML(da);	/*converts 'da' to xml stream of data*/
		System.out.println("The XML Output is:");
		System.out.println(xml);
		sendToBC(xml, serverName);
		xmlFile = new File(XMLfileName); /*file pointer for the XML file*/
		if (xmlFile.exists() == true && xmlFile.delete() == false){	/*if the file exists and the deletion is not possible*/
	    throw (IOException) new IOException("XML file delete failed.");
		}
		xmlFile = new File(XMLfileName);
		if (xmlFile.createNewFile() == false){
	    throw (IOException) new IOException("XML file creation failed.");/*if the file could not be created*/
		}
		else{
	    toXmlOutputFile = new PrintWriter(new BufferedWriter(new FileWriter(XMLfileName)));
	    toXmlOutputFile.println("First arg to Handler is: " + argOne + "\n");	/*first argument in the mimer.output will be 'WillBeFileName'*/
	    toXmlOutputFile.println(xml);/*we will be writing the data into the XML output file*/
	    toXmlOutputFile.close();/*closing the output stream to ssave resources*/
		}
	}catch(IOException ioe){ioe.printStackTrace();}
  }
  
  static void sendToBC (String sendData, String serverName){
    Socket sock;
    BufferedReader fromServer;
    PrintStream toServer;
    String textFromServer;
    try{
      // Open our connection Back Channel on server:
      sock = new Socket(serverName, 2570);/*we wll be listening at port 2570*/
      toServer   = new PrintStream(sock.getOutputStream()); /*output stream to teh MyWebServer or any program that conects to the port 2570*/
      // Will be blocking until we get ACK from server that data sent
      fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));/*input stream*/
      
      toServer.println(sendData);	/*sending data to the client listening at 2570*/
      toServer.println("end_of_xml");/*Mark the end of file*/
      toServer.flush(); 
      // Read two or three lines of response from the server,
      // and block while synchronously waiting:
      System.out.println("Blocking on acknowledgment from Server... ");
      textFromServer = fromServer.readLine();
      if (textFromServer != null){System.out.println(textFromServer);}/*display if anything has been returned from the client listening at 2570*/
      sock.close(); /* close the socket*/
    } catch (IOException x) {
      System.out.println ("Socket error.");
      x.printStackTrace ();
    }
  }
}
