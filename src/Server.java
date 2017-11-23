/**
 *
 * @author Setup
 */
 
import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class Server
{

    private int portNumber;
    private ServerSocket serviceSocket;
    private Socket clientSocket;
    //Used
    private boolean serverRunning;
    //Used to keep track of what thread calls methods
    public AtomicInteger id;

    LinkedList<clientConnectors> clients;

    public Server(int portNumber)
    {
        this.portNumber = portNumber;
        serverRunning = true;
        clients = new LinkedList<clientConnectors>();
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
    
    //Broadcasts client messages and their departures on the chatroom
    private synchronized void transmission(String message, int id, String username)
    {
        if (message != null && !message.equals("exit()"))
        {
            transmitMessage(message, id, username);
        } else
        {
            transmitDeparture(id, username);
        }
    }
    
    //sends a message to all the clients on the chatroom
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
                } 
                catch (IOException ex)
                {
                    System.out.println("Problem broadcasting to client " + username);
                }
            } 
            else
            {
                //does not need to be implemenet

                /*try 
                    {
                        client.output.writeUTF("You: "+message);
                    }
                    catch (IOException ex) 
                    {
                       System.out.println("Problem broadcasting to client "+username);
                    }*/
            }
        }
    }
    
    //sends the news of departure to all the clients on the chatroom
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
                        client.output.writeUTF("Client: " + username + " has disconnected");
                    }
                } 
                catch (IOException ex)
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
    if clients do not have a username (i.e. they disconnected) then server terminates their conncection
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
                        client.output.writeUTF("Welcome: " + username);
                        client.output.writeUTF("Enter any message to the server, type exit() to disconnect");
                    } 
                    catch (IOException ex)
                    {
                        System.out.println("Problem broadcasting to client");
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
    
    //disconnects all clients
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
            //first message from the client becomes its username
            username = readIncomingMessage();
            welcome(username, getUniqueID());

            if (hasUsername())
            {
                System.out.println("UniqueID: " + uniqueID + " is now known as: " + username);
            }

            while (clientConnectorRunning)
            {
                String message = readIncomingMessage();
                transmission(message, getUniqueID(), username);
                message = "";
            }
        }
        
        // reads the messages from the clients using their input streams
        public String readIncomingMessage()
        {
            try
            {
                String message = input.readUTF();
                return message;
            } catch (IOException ex)
            {
                if (hasUsername())
                {
                    System.out.println("Client disconnected: " + username);
                }
                return null;
            }
        }
        
        // disconnects particular client from the server
        public void disconnect()
        {
            stopProcess();
            try
            {
                clientSocket.close();
                output.close();
                input.close();

            } 
            catch (IOException ex)
            {
                System.out.println("Error closing connection");
            }
        }
        
        //terminates the thread
        private void stopProcess()
        {
            clientConnectorRunning = false;
        }
    }

    //listens to the server command line to see if an exit command is given
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
