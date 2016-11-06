import java.net.*;
import java.io.*;
import java.util.*;

class Worker extends Thread{
	Socket sock;
	Worker (Socket s){
		sock = s;
	}
	public void run(){
		try{
			PrintStream out = new PrintStream(sock.getOutputStream());	/*Output Stream to the browser*/
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));	/*Input stream from the browser*/
			String browserRequest = in.readLine();	/*HTTP Get request will be stored in this variable*/
			String requestedFile;
			String contentType = ""; 
			StringTokenizer tokens = new StringTokenizer(browserRequest, " ");	/* We are tokenising the request and also passing the deliminator which is space*/
			String request = tokens.nextToken();	/*to check whether the Get or Post method has been called, Usually the first token is this method*/ 
			if(request.equalsIgnoreCase("get") == true){	/*if the get method was called*/
				requestedFile = tokens.nextToken();	/*which file has been requested*/
				if(requestedFile.endsWith("..") == true){
					out.print("STAY WITHIN YOUR LIMITS");	
					RuntimeException r = new RuntimeException();	/*the browser wants to open the up one directory from the current directory */
					throw r;
				}
			}else {
				requestedFile = null;	/*If the request was not GET then the requested file will be null*/
			}
			if(requestedFile == null)	/*if the requested file is null then the request is not GET*/
			{
				RuntimeException re = new RuntimeException();	/*if its not a GET request then generate another error */
				
				System.out.println("Not a GET request\n");
				throw re;
			}
			if (requestedFile.endsWith(".html") == true){
				contentType = "text/html";
				display(requestedFile, out, contentType);	/*this fuction will send the html file to the user using the output stream which is also being sent as a parameter*/
			}
			else if(requestedFile.endsWith(".txt") == true){
				contentType = "text/plain";
				display(requestedFile, out, contentType);/*this fuction will send the txt file to the user using the output stream which is also being sent as a parameter*/
			}
			else if(requestedFile.endsWith("/") == true){	
				contentType = "text/plain";
				displayDirectory(requestedFile, out, contentType);/*this function will display the directory*/
			}
			else if(requestedFile.endsWith("/cgi/addnums.fake-cgi") == true)
			{
				contentType = "text/html";
				//htmlFormAdd(requestedFile, out, contentType);
			}
			else{
				contentType = "text/plain";
				display(requestedFile, out, contentType);	/*this basically wot return anything*/
			}
			sock.close();
		}catch(IOException ioe){	/*If any Input Output Exception is generated*/
			System.out.println(ioe);
		}
		
		}
		
		public void displayDirectory(String requesteddirectory, PrintStream out, String contentType) throws IOException{	/*This function handles the display of the current directory in which the .class file is present*/
			String directory = requesteddirectory;
			BufferedWriter fileDisplay = new BufferedWriter(new FileWriter("fileDisplay.html"));/* we are creating a html file which will be used to display the layout of the directory*/
			
			fileDisplay.write("<html><head>Display Directory</head><br><br>");/*we are writing the html code into the fileDisplay.html*/
			fileDisplay.write("<font size=100><font color = #00HHFF> Directory: " + requesteddirectory + "</font></font>" + "<br>");
			fileDisplay.write("<a href=\""+"http://localhost:2540"+"/\">"+"Back to Root"+"</a>");
			
			File firstDirectory = new File("./"+ requesteddirectory +"/");	/*we are creating a new File instance*/
			File[] directoryFiles = firstDirectory.listFiles();	/*we are storing the files present in the current directory in the directoryFiles array*/
			for(int i=0;i<directoryFiles.length;i++)	/*to display all the files in the current directory*/
			{
				String fileName = directoryFiles[i].getName();	/*retrieving the filename present in the current index number*/
				if(fileName.startsWith(".") == true)
					continue;
				if(fileName.startsWith("fileDisplay.html") == true)	/*the fileDisplay file is what we created at the start of this function and hence we need not display this file*/
					continue;
				if(directoryFiles[i].isFile() == true){
					fileDisplay.write("<li>"+"<a href=\""+fileName+"\">"+ fileName + "</a></li><br>");	/*if the current file is a file then it will be displayed be it .html or .txt*/
				}
				if(directoryFiles[i].isDirectory() == true)
					fileDisplay.write("<li>"+"<a href=\"" + fileName + "/\">/" + fileName + "</a></li> <br>");	/*if the current index location points towards a directory*/
				fileDisplay.flush();
			}
			fileDisplay.write("</body></html>"); /* we are closing the html file which we had created at the start of the function*/
			File tempFile = new File("fileDisplay.html");
		
			InputStream stream = new FileInputStream("fileDisplay.html");
			out.println("HTTP/1.1 200 OK" + "Content-Length: " + tempFile.length() + "Content-Type: "  + contentType + "\r\n\r\n");  // displaying the header at the end of the page on the browser*/
		
			System.out.println("SASi's server is sending directory: " + directory); /* This will be displayed on the command prompt*/
		
			byte[] displayFileBytes = new byte[15656]; /* this array will hold 15656 bytes*/
			int numberOfBytes = stream.read(displayFileBytes);  /*to get the size of the html file which we created, to send it as part of the header*/
			out.write(displayFileBytes, 0, numberOfBytes);  /*send the display file bytes to browser as they are without any formatiing*/
  
			fileDisplay.close(); /* close bufferedWriter*/
			out.flush(); 
			stream.close(); /* we are closing the input stream*/
			tempFile.delete(); /*we are deleting the memory assigned to the tempFile ie garbage collection */
			
		}
		
		/*public void htmlFormAdd(String fromBrowser, PrintStream out, String contentType) throws IOException{
			System.out.println(fromBrowser);
			if(!fromBrowser.contains("?") || fromBrowser.substring(fromBrowser.indexOf('?')+1) == null || fromBrowser.length() == 0){
				out.println("BAD REQUEST");
			}
			StringTokenizer token = new StringTokenizer(fromBrowser, "&");	/*We are tokenising the whole request, splitting on &*/
			/*Map<String,String> paramMap = new HashMap<String,String>();
			String person;
			int n1,n2,result;
			try{
				String num1,num2;	/*initially in the request the num1, num2 and person are used in the form and they'll be received as string format by this program*/
			/*	if((person = paramMap.get(person)) == null || (num1 = paramMap.get(num1)) == null || (num2 = paramMap.get(num2)) == null){
					out.println("BAD REQUEST, no fields should be empty");
					return;
				}
			n1 = Integer.parseInt(num1);	//converting the string to integer
			n2 = Integer.parseInt(num2);
			result = n1+n2;
			System.out.println("addnums - person:"+person+"\tnum1:"+num1+"\tnum2:"+num2+"\tresult:"+result);
			}catch( Exception ue){
				ue.printStackTrace();
			}
			out.println("HTTP/1.1 200 OK Content-Type: " + contentType + "\r\n\r\n");  /*content type display on the browser*/
			/*out.println("<html><head></head><body>"); /* begininng of the html format*/
			/*out.println("<p>Hello\t"+person+"The sum of: "+n1+" and "+n2+" is:"+result);
		}*/
		
		public void display(String requestedFile, PrintStream out, String contentType) throws IOException{	/*this function will handle the display of the html and txt files*/
			if(requestedFile.startsWith("/") == true) /*when we get the GET request from browser there will be '/' separating the port number and file name nd hence we are creating a substring for it*/
				requestedFile = requestedFile.substring(1); /*we are removing the '/'*/
			try{
				FileInputStream fis = null;	/*Input Stream to read from the file*/
				BufferedReader reader = null; 
				try{
					
					fis = new FileInputStream(requestedFile);
					reader = new BufferedReader(new InputStreamReader(fis));	/*reader will contain a line form the file*/
					
					String line = reader.readLine(); /*the line which we read from the file will be stored in this variable*/
					while(line!=null){ /*loop will iterate till the time the end of file is not reached */
						out.println(line);	/*we are sending the line which we read using the output stream whic was passed as parameter to this fuction*/
						line = reader.readLine(); /*reading the next line*/
					}
					File f = new File(requestedFile);	/*the File instance of the requested file*/
					System.out.println("SASi's server is sending the following FILE:"+requestedFile+"\nContent Type:"+contentType+"\nContent Length:"+f.length()+"\r\n\r\n");/*these details will be displayed on the server side*/
					out.println("HTTP/1.1 200 OK\nContent-Type:"+contentType+"Content Length:"+f.length()+"\r\n\r\n");/*This header format will be sent to the browser so that *it can be displayed after the requested file has been displayed*/
					//f.close();
				}catch( FileNotFoundException ex){
				System.out.println(ex);	/*if the file could not be located then this msg will be displayed o the command prompt*/
				}
				reader.close();	/*we are closing the reader*/
				fis.close();/*closing the file input stream*/
			}catch(IOException ioe){ /*if Input output exception has been generated then the printstacktrace will determine the place where it has been generated*/
				ioe.printStackTrace();
			}
		}
	}
public class MyWebServer{
	public static void main(String[] args) throws IOException{
		int port = 2540; /*the webserver is listening at port 2540*/
		int q_len = 5;	/*backlog size we have declared it as 5*/
		ServerSocket servsock = new ServerSocket(port, q_len);	/*creating an instance of serversocket*/
		Socket sock; /*creating the socket*/
		System.out.println("Sasi's WebServer is listening at port:\t"+port+"\n");
		while(true){ /*continue listening*/
			sock = servsock.accept(); /*accepting all the requests coming in*/
			new Worker(sock).start(); /*starting a thread to handle the present request*/
		}
	}
}