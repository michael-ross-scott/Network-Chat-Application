import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Michael Scott
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
    // used to terminate the program from other threads
    boolean running;
    int flag = 0;
    boolean firstRunRead = false;
    boolean firstRunWrite = false;
    String incomingMessage;
    
    // restricted username list
    private String [] restricted = new String[]
    {"exit()", "Upload:", "Download:", "~~Download Incoming~~", "ListImages()", "ListUsers()"};
    
    public Client(int port,String host)
    {
        this.port = port;
        this.host = host;
        message="";
    }
    
    public void process()
    {
        try 
        {
            running = true;
            
            clientSocket = new Socket(host,port);
            System.out.println("\nClient connected");
            connectStreams();
            
            messageWriter mw = new messageWriter();
            mw.start();
            
            // Server listener
            while(running)
            {
                
                try 
                {
                		// If the flag is set to 1, the client must receive the image sent courtesy of the server
                    if(flag == 1)
                    {
                        receiveImage();
                        flag = 0;
                    }
                    // Reads the incoming messages 
                    else
                    {
                        incomingMessage = input.readUTF();
                        System.out.println(incomingMessage);
                        if(firstRunRead == false)
                        {
                            System.out.println("Your working directory is: "+getWorkingDirectory());
                            firstRunRead = true;
                        }
                    }
                } 
                
                catch (IOException ex) 
                {
                    System.out.println("Server has closed the connection, Press enter to terminate client.");
                    // Terminates messageWriter
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
    
    // Gets the working directory and prints it out for uploading purposes
    private String getWorkingDirectory()
    {
        File f = new File("test.txt");
        String path = f.getAbsolutePath();
        String absolutePath = path.substring(0,path.lastIndexOf("/")+1);
        return absolutePath;
    }
    
    // Reads the message written in the terminal
    private void readMessage()
    {
        if(!clientSocket.isClosed())
        {
            
            if(firstRunWrite == false)
            {
                Scanner sc = new Scanner(System.in);
                message = sc.nextLine();
                
                while(reservedUsername(message))
                {
                    message = sc.nextLine();
                    System.out.println("text is a reserved word ");
                }
                firstRunWrite = true;
                return;
            }
            
            Scanner sc = new Scanner(System.in);
            message = sc.nextLine();
        }
    }
    
    // Checks if the client enters a valid username (can't be an empty string or restricted word)
    private boolean reservedUsername(String message)
    {
        if(message.equals(""))
        {
            return true;
        }
        
        // Checks if the username is a part of restricted key words
        for(int i = 0; i < restricted.length; i++)
        {
            if(message.equals(restricted[i]))
            {
                return true;
            }
        }
        return false;
    }
    
    //Writes the message that user entered in, sends a file that the user selects or downloads a file
    private boolean writeMessages()
    {
        if(!clientSocket.isClosed())
        {
            // Sends normal message
            if(!message.startsWith("Upload") && !message.startsWith("Download"))
            {
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
            // Uploads a file
            else if(message.startsWith("Upload"))
            {
                try
                {
                    String path = message.substring(message.indexOf(" ")+1, message.length());
                    BufferedImage image = ImageIO.read(new File(path));
                    
                    try
                    {
                        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                        ImageIO.write(image, "jpg", byteOutput);
                        
                        byte[] size = ByteBuffer.allocate(4).putInt(byteOutput.size()).array();
                        
                        System.out.println("\nClient: Uploading...");
                        
                        output.writeUTF("~~~Download incoming~~~");
                        output.write(size);
                        output.write(byteOutput.toByteArray());
                        output.flush();
                        
                        System.out.println("Server: Upload successful.\n");
                        
                        return true;
                    }
                    catch (IOException ex)
                    {
                        System.out.println("Client: Image too big");
                    }
                } 
                catch (IOException ex)
                {
                    System.out.println("Client: Image cannot be found\n");
                    return true;
                }
            }
            // Downloads the image 
            else
            {
                try
                {
                    output.writeUTF(message);
                    flag = 1;
                } 
                catch (IOException ex)
                {
                    System.out.println("Connection lost with server");
                }
                return true;
            }
        }
        return false;
    }
    
    // Receives the image from the server
    public void receiveImage()
    {
        try
        {
            if(!incomingMessage.startsWith("Problem"))
            {
                byte[] sizeAr = new byte[4];
                
                input.read(sizeAr);
                int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();

                byte[] imageAr = new byte[size];
                input.read(imageAr);
                
                ByteArrayInputStream bIn = new ByteArrayInputStream(imageAr);
                BufferedImage image = ImageIO.read(bIn);
                File f = new File("clientImg.jpg");
                ImageIO.write(image, "jpg", f);
            }
            else
            {
                System.out.println("Problem saving image\n");
            }
        } 
        catch (IOException ex)
        {
            System.out.println("Problem saving image.");
        }
        
    }
    
    // Disconnects client from the server
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
    
    // Connects input and output streams
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
