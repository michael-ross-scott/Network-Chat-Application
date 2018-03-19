import java.awt.image.BufferedImage;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class Server
{

    private int portNumber;
    private ServerSocket serviceSocket;
    private Socket clientSocket;
    // Used to keep track of server alive state
    private boolean serverRunning;
    // Used to keep track of what thread calls methods
    public AtomicInteger id;
    // Used to track clients
    LinkedList<clientConnectors> clients;
    // Used to track uploads
    ArrayList<File> files;

    public Server(int portNumber)
    {
        this.portNumber = portNumber;
        serverRunning = true;
        clients = new LinkedList<clientConnectors>();
        files = new ArrayList<File>();
        id = new AtomicInteger(0);
    }

    /* 
    Starts a server listener which scans the console for an exit message and 
    starts client-server connections as multiple threads 
    */
    public void runServer()
    {
        try
        {
            System.out.println("Waiting for incoming connections...");
            serviceSocket = new ServerSocket(portNumber);
            serverListener sl = new serverListener();
            sl.start();

            while (serverRunning)
            {
                clientSocket = serviceSocket.accept();
                clientConnectors client = new clientConnectors(clientSocket);
                clients.add(client);
                client.start();
                System.out.println("Connection established with uniqueID: " + client.getUniqueID());
            }
        } 
        catch (IOException ex)
        {
            System.out.println("Problem establishing the server");
        }
    }

    // Broadcasts client messages and their departures on the chatroom
    private synchronized void transmission(String message, int id, String username)
    {
        // User left chatroom
        if (message == null || message.equals("exit()"))
        {
            transmitDeparture(id, username);
        } 
        // User downloading image
        else if (message.startsWith("~~~Download incoming~~~"))
        {
            setImageReceive(id);
        }
        // User requesting an image to download
        else if (message.startsWith("Download:"))
        {
            String path = message.substring(message.indexOf(" ") + 1, message.length());
            sendImage(id,path);
        }
        // User asking server what images they have
        else if(message.startsWith("ListImages()"))
        {
            sendImageList(id, username);
        }
        // User direct messaging another user
        else if(message.startsWith("DM"))
        {
            int index = message.indexOf(" ") + 1;
            int secondIndex = message.indexOf(" ", index + 1);
            
            String toUser = message.substring(index,secondIndex);
            String actualMessage = message.substring(secondIndex, message.length());
            
            sendDirectMessage(id,username,toUser,actualMessage);
        }
        // User requesting list of users from chatroom
        else if(message.startsWith("ListUsers()"))
        {
            listUsers(id);
        }
        // User sending normal message
        else
        {
            transmitMessage(message, id, username);
        }
    }
    
    // Lists users in chatroom
    private void listUsers(int id)
    {
        String clientsPresent = "\nServer: Users Present-\nYou\n";
        
        for(int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);
            
            if(id != client.getUniqueID())
            {
                if(client.hasUsername())
                {
                    clientsPresent+=client.getUsername()+"\n";
                }
                else
                {
                    clientsPresent+="Username pending\n";
                }
            }
        }
        
        for(int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);
            
            if(id == client.getUniqueID())
            {
                try
                {
                    client.output.writeUTF(clientsPresent);
                } 
                catch (IOException ex)
                {
                    System.out.println("Connection lost with client: "+client.getUniqueID());
                }
            }
        }
    }
    
    // Send messages directly in chatroom to another client
    private boolean sendDirectMessage(int id, String sourceUser, String destUser, String message)
    {
        for(int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);
            String tempUser = client.getUsername();
            
            if(destUser.compareTo(tempUser) == 0)
            {
                try
                {
                    client.output.writeUTF(sourceUser+"-DM: "+message);
                    return true;
                } 
                catch (IOException ex)
                {
                    System.out.println("Direct message to username: " + destUser + " from: " + sourceUser + " failed");
                    return false;
                }
            }
        }
        
        for (int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);
            if (id == client.getUniqueID())
            {
                try
                {
                    client.output.writeUTF("user: " + destUser + " does not exist");
                } 
                catch (IOException ex) 
                {
                    System.out.println("Client cannot be reached");
                }
            }
        }
        return false;
    }
    
    // Send list of images on server
    private void sendImageList(int id, String username)
    {
        for (int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);
            if (id == client.getUniqueID())
            {
                try 
                {
                    String message = outputAllFiles();
                    client.output.writeUTF(message);
                } 
                catch (IOException ex) 
                {
                    System.out.println("Connection lost with client " + client.getUniqueID());
                }
            }
        }
    }
    
    // Textual representation of all the files on the server 
    private String outputAllFiles()
    {
        String temp="\nServer: list of files is as follows:\n";
        for(int i = 0; i < files.size(); i++)
        {
            temp += files.get(i) + " with file size: " + files.get(i).length() + " kb\n";
        }
        
        if(files.size()==0)
        {
            temp += "There are no files stored on the server\n";
        }
        return temp;
    }
    
    // Find client to send the image to
    private void sendImage(int id,String name)
    {
        for (int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);
            if (id == client.getUniqueID())
            {
                sendActualImage(client.getOutputStream(),name,client);
            }
        }
        
    }
    
    // Send image to the client
    private void sendActualImage(DataOutputStream output,String name, clientConnectors client)
    {
        try
        {
            String path = System.getProperty("user.dir");
            String finalPath = path + "/" + name;
            
            BufferedImage image = ImageIO.read(new File(finalPath));
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            
            ImageIO.write(image, "jpg", byteOutput);
            
            byte[] size = ByteBuffer.allocate(4).putInt(byteOutput.size()).array();
            
            output.writeUTF("Client: Receiving image...");
            output.write(size);
            
            byte [] byteArr = byteOutput.toByteArray();
            
            output.write(byteArr);
            output.flush();
            
            output.writeUTF("Client: Image received");
        }
        catch (IOException ex)
        {
            System.out.println("Problem sending image to: "+client.getUsername());
            
            try
            {
                output.writeUTF("Problem reading in the image.");
            }
            catch (IOException ex1)
            {
                System.out.println("Connection lost with client: "+client.getUsername());
            }
        }
    }
		
	   // Set the client up to receive the image from the server
    private void setImageReceive(int id)
    {
        for (int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);
            if (id == client.getUniqueID())
            {
                client.setFlag();
            }
        }
    }

    // Sends a message to all the clients on the chatroom
    private void transmitMessage(String message, int id, String username)
    {
        for (int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);
            if (id != client.getUniqueID())
            {
                try
                {
                    client.output.writeUTF(username + ": " + message);
                } catch (IOException ex)
                {
                    System.out.println("Problem broadcasting to client " + username);
                }
            }
        }
    }

    // Sends the news of departure to all the clients on the chatroom
    private void transmitDeparture(int id, String username)
    {
        clientConnectors target = null;

        for (int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);

            if (id == client.getUniqueID())
            {
                target = client;
            } 
            else
            {
                try
                {
                    if (client.hasUsername())
                    {
                        client.output.writeUTF("Server: Client " + username + " has disconnected");
                    }
                } catch (IOException ex)
                {
                    // Should not get here
                    System.out.println(ex);
                }
            }
        }
        clients.remove(target);
        target.disconnect();
    }

    /*
    Default message sent to all clients when they connect to the server, 
    if clients do not have a username (i.e. they disconnected) then server terminates their connection
     */
    private void welcome(String username, int id)
    {
        if (username != null)
        {
            for (int i = 0; i < clients.size(); i++)
            {
                clientConnectors client = clients.get(i);
                if (id == client.getUniqueID())
                {
                    try
                    {
                        client.output.writeUTF("\nServer: Welcome- " + username);
                        client.output.writeUTF("Enter any message to the server, type exit() to disconnect");
                        client.output.writeUTF("\nCOMMANDS:\nUpload: IMAGE_NAME uploads an image.\n"
                                + "Download: IMAGE_NAME downloads an image.\n"
                                + "ListImages() lists all images stored on the server.\n"
                                + "ListUsers() Lists all the users currently on the chatroom.\n");
                    } 
                    catch (IOException ex)
                    {
                        System.out.println("Problem broadcasting to client "+ client.getUsername());
                    }
                }
            }
        } 
        else
        {
            for (int i = 0; i < clients.size(); i++)
            {
                clientConnectors client = clients.get(i);
                if (id == client.getUniqueID())
                {
                    clients.remove(client);
                    client.disconnect();
                }
            }
        }
    }
		
	   // Receive the image from the client
    public synchronized void receiveImage(int id)
    {
        clientConnectors finalClient = null;
        for (int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);
            if (id == client.getUniqueID())
            {
                finalClient = client;
            }
        }
        try
        {
            DataInputStream input = finalClient.getInputStream();

            byte[] sizeArr = new byte[4];
            input.read(sizeArr);
            int size = ByteBuffer.wrap(sizeArr).asIntBuffer().get();

            byte[] imageAr = new byte[size];
            
            input.read(imageAr);
            ByteArrayInputStream bIn = new ByteArrayInputStream(imageAr);
            BufferedImage img = ImageIO.read(bIn);

            System.out.println("Received image from username: "+finalClient.getUsername());
            File f = new File("serverImage.jpg");
            ImageIO.write(img, "jpg", f);

            files.add(f);
        } 
        catch (IOException ex)
        {
            System.out.println("Server: Problem uploading the file from "+finalClient.getUsername());
        }
    }

    // Disconnects all clients
    private void disconnectAll()
    {
        for (int i = 0; i < clients.size(); i++)
        {
            clientConnectors client = clients.get(i);
            client.disconnect();
            clients.remove(client);
        }
    }

    /* 
     Connectors to clients, this is multi-threaded due to the fact that the server needs to assign ports
     to connections dynamically as it runs
     */
    class clientConnectors extends Thread
    {

        DataInputStream input;
        DataOutputStream output;
        Socket clientSocket;
        boolean clientConnectorRunning;
        private int uniqueID;
        private String username;
        private int flag = 0;

        public void setFlag()
        {
            flag = 1;
        }

        public void resetFlag()
        {
            flag = 0;
        }

        public DataOutputStream getOutputStream()
        {
            return output;
        }
        
        public String getUsername()
        {
            return ""+username;
        }

        public DataInputStream getInputStream()
        {
            return input;
        }

        public int getFlag()
        {
            return flag;
        }

        public boolean hasUsername()
        {
            if (username == null)
            {
                return false;
            }
            return true;
        }

        public int getUniqueID()
        {
            return uniqueID;
        }

        public clientConnectors(Socket clientSocket)
        {
            this.clientSocket = clientSocket;
            clientConnectorRunning = true;
            uniqueID = id.incrementAndGet();

            try
            {
                output = new DataOutputStream(clientSocket.getOutputStream());
                input = new DataInputStream(clientSocket.getInputStream());
            } 
            catch (IOException ex)
            {
                System.out.println("Problem connecting to the client");
            }
        }

        public void run()
        {
            // First message from the client becomes its username
            username = readIncomingMessage();
            welcome(username, getUniqueID());

            if (hasUsername())
            {
                System.out.println("UniqueID: " + uniqueID + " is now known as: " + username);
            }

            while (clientConnectorRunning)
            {
            	   // Receive messages
                if (flag == 0)
                {
                    String message = readIncomingMessage();
                    transmission(message, getUniqueID(), username);
                    message = "";
                } 
                // Receive image
                else if (flag == 1)
                {
                    receiveImage(getUniqueID());
                    resetFlag();
                    File f = files.get(files.size() - 1);
                    String message = "has sent image to the server of size: " + f.length() + " bytes. The file is named " + f.getName()
                            + " ,to download type Download: IMAGE_NAME";
                    transmission(message, getUniqueID(), username);
                }
            }
        }

        // Reads the messages from the clients using their input streams
        public String readIncomingMessage()
        {
            try
            {
                String message = input.readUTF();
                return message;
            } 
            catch (IOException ex)
            {
                if (hasUsername())
                {
                    System.out.println("Client disconnected: " + username);
                }
                return null;
            }
        }

        // Disconnects particular client from the server
        public void disconnect()
        {
            stopProcess();
            try
            {
                clientSocket.close();
                output.close();
                input.close();

            } catch (IOException ex)
            {
                System.out.println("Error closing connection");
            }
        }

        // Terminates the thread
        private void stopProcess()
        {
            clientConnectorRunning = false;
        }
    }

    // Listens to the server command line to see if an exit command is given
    class serverListener extends Thread
    {

        DataInputStream input;
        String serverMessage = "";

        private void readMessage()
        {
            Scanner sc = new Scanner(System.in);
            serverMessage = sc.nextLine();
        }

        public void run()
        {
            while (!serverMessage.equals("exit()"))
            {
                readMessage();
            }
            serverRunning = false;
            disconnectAll();
            System.exit(0);
        }
    }

    public static void main(String[] args)
    {
        System.out.println("Enter a port number > 2000, remember this port number for when you create the clients");

        Scanner sc = new Scanner(System.in);
        int portNumber = sc.nextInt();

        Server s = new Server(portNumber);
        s.runServer();
    }
}

