package rmi;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.*;
import java.util.*;

import javax.swing.JFrame;

public class StartClient 
{

	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocation(new Point(800,400));
		frame.setSize(new Dimension(500, 450));
		frame.setTitle("Chat System");
		frame.setVisible(true);
		
		try {

			System.setSecurityManager(new RMISecurityManager());
			GroupChatInterface server = (GroupChatInterface) Naming.lookup("rmi://localhost/ABCD");

			Scanner scanner = new Scanner(System.in);
			System.out.println("[System] Client Messenger is running");
			System.out.println("Enter a username to login and press Enter:");
			String username = scanner.nextLine();
			MessengerInterface m = new Messenger(username, server);
			server.login(m);
			server.sendToAll("Just Connected", m);
			for (;;) 
			{
				String aa = scanner.nextLine();
				server.sendToAll(aa, m);
			}
		} catch (Exception e)
		{
			System.out.println("Hello Client exception: " + e);
			e.printStackTrace();
		}
	}
}