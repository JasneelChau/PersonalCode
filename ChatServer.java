package Chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;


public class ChatServer 
{
	private String message;
	   
	   public ChatServer(String message)
	   {  this.message = message;
	   }
	   
	   public String getGreeting()
	   {  
		   System.out.println("getGreeting method called");
	       return message;
	   }

    private static final int PORT = 9002;

    private static HashSet<String> names = new HashSet<String>();

    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    public static void main(String[] args) throws Exception 
    {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
        
        ChatServer remoteObject = new ChatServer("Hello Remote World");
        try
        {  
      	   LocateRegistry.createRegistry(1099); 

           RMIGreeting stub = (RMIGreeting)UnicastRemoteObject.exportObject(remoteObject, 0);
   
           Registry registry = LocateRegistry.getRegistry();
           registry.rebind("greeting", stub);
        }
        catch (RemoteException e)
        {  System.err.println("Unable to bind to registry: " + e);
        }
        // note that separate thread created to keep remoteObject alive
        System.out.println("Main method of RMIGreetingImpl done");
    }

 
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {

                
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }


                out.println("NAMEACCEPTED");
                writers.add(out);


                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + ": " + input);
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {

                if (name != null) {
                    names.remove(name);
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}