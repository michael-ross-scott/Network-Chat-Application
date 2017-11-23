import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 *
 * @author Setup
 */
public class Client
{
    /// RECEIVE responses from the server
    private DataInputStream input;
    
    // CREATE responses to the server
    private DataOutputStream output; 
    
    private Socket clientSocket;
    private final int port;
    private final String host;
    
    private String message;
    //used to terminate the program from other threads
    boolean running;
    
    public Client(int port,String host)
    {
        this.port = port;
        this.host = host;
    }
    
    public void process()
    {
        try {
            running = true;
            
            clientSocket = new Socket(host,port);
            System.out.println("Client connected");
            connectStreams();
            
            messageWriter mw = new messageWriter();
            mw.start();
            
             while(running)
             {
                String incomingMessage;
                try 
                {
                    incomingMessage = input.readUTF();
                    System.out.println(incomingMessage);
                } 
                
                catch (IOException ex) 
                {
                    System.out.println("Server has closed the connection, Press enter to terminate client.");
                    //terminates messageWriter
                    running = false;
                    disconnect();
                }
            }
        } 
        catch (IOException ex) 
        {
            System.out.println("Trouble connecting to the server.");
        }
    }
    
    //Reads the message written in the terminal
    private void readMessage()
    {
        if(!clientSocket.isClosed()){
            Scanner sc = new Scanner(System.in);
            message = sc.nextLine();
        }
    }
    
    //Writes the message that user entered in, in readMessage
    private boolean writeMessages()
    {
        if(!clientSocket.isClosed()){
            try 
            {
                output.writeUTF(message);
                output.flush();
                return true;
            } 
            catch (IOException ex) 
            {
                System.out.println("Connection lost with server");
                return false;
            }
        }
        return false;
    }
    
    //disconnects client from the server
    private void disconnect()
    {
        try 
        {
            output.close();
            input.close();
            clientSocket.close();
        } 
        catch (IOException ex) 
        {
            System.out.println(ex);
        }
    }
    
    //connects input and output streams
    private void connectStreams()
    {
        try 
        {
            System.out.println("Input/Output Streams connected");
            input = new DataInputStream(clientSocket.getInputStream());
            output = new DataOutputStream(clientSocket.getOutputStream());
        } 
        catch (IOException ex) 
        {
            System.out.println("Trouble connecting input/output streams");
        }
    }
    
    /*
      Receiving messages are multi-threaded because the client needs to ALWAYS listen to the server
      for messages from other clients.
    */
    class messageWriter extends Thread{
        
        public void run()
        { 
            System.out.println("Enter your username to the server.");
            while(running == true)
            {
                readMessage();
                while(!message.equals("exit()"))
                {
                    if(writeMessages() != true)
                    {
                        break;
                    }
                    readMessage();
                }
                disconnect();
                running=false;
            }
        }
    }
    
    public static void main(String[] args) throws UnknownHostException
    {
        String hostName = InetAddress.getLocalHost().getHostAddress();
        
        System.out.println("Enter a port number > 2000, then push enter");
        Scanner sc = new Scanner(System.in);
        int portNumber = sc.nextInt();
        
        Client first = new Client(portNumber, hostName);
        first.process();
    }
}
