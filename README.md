# Network-Chat-Application

Simple client-server chat application developed in java with built-in support for compilation via Linux command line.

# Description

This chat application follows a chatroom design, messages sent from clients are broadcasted to all other memebers in the chatroom. In addition members can upload images to the server and download images from it. Members of the chatroom also have the ability to direct message each other.

# Class Structure

Client

  - Purpose: Allows the sending and receiving of messages and images via the command line.
  
FirstClient

  - Purpouse: Class that facilitates easy running of a second client instance (useful if you are on one machine).
  
Server:

  - Purpouse: Handles client activity (i.e. message transmission) and file management
  
# Client commands:

-> exit()                           -      Exits the chatroom and disconnects the client.

-> ListUsers()                      -      Lists all the users currently in the chatroom.

-> Upload: ABSOLUTE_IMAGE_PATH      -      Uploads the image, at the image path specified, to the server.

-> Download: IMAGE_NAME             -      Downloads the image of the name specified.

-> DM USERNAME MESSAGE              -      Direct messages the MESSAGE to the user with username USERNAME

# Running project

  1. Open Command Line
  2. Input 'make all'
  3. Open three seperate command line windows (one for server, two for clients)
    
    3.1 Alternatively you can run the programs on seperate computers connected to the same network
    3.2 Please ensure the ports you give the program are open if you are running on seperate computers
    
  4. Navigate to /bin (cd bin) and input 'java server' (1st command line window)
  
    4.1. Follow instructions of the server program and stop when it outputs "awaiting connections"
    
  5. Navigate to /bin (cd bin) and input 'java client' (2nd command line window)
  
    5.1 Follow instructions of the client program until connected to the server
    
  6. Navigate to /bin (cd bin) and input 'java firstClient' (3rd command line window)
  
    6.1 Follow instrutions of the client program until connected
    
  7. Note step 5/6 can be repeated any number of times to populate the chatroom
  8. You can run the project in an IDE, but the necessary imports etc are then up to the user.
