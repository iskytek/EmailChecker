package emailchecker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
        
public class EmailChecker 
{
    private String emailAddress;
    private Socket MXServer;
    private InputStreamReader serverIn = null;
    private OutputStream serverOut = null;

    public static void main(String[] args) 
    {
        EmailChecker ec = new EmailChecker();
        String serverName = ec.getMXServer();
        ec.connectToServer(serverName);
        if(ec.MXServer != null)
        {
            ec.getStreams();
            ec.processCommands();
            ec.cleanUp();
        }
        

    }
    
    private String getMXServer()
    {
        String MXServerName = null;
        boolean validServer = false;
            while(!validServer)
            {
                try
                {
                    InitialDirContext idc = new InitialDirContext();

                    Scanner sc = new Scanner(System.in);


                        System.out.print("Enter the email address: ");
                        emailAddress = sc.nextLine();
                        if(!validateEmail(emailAddress))
                        {
                            validServer = false;
                            System.out.println("Invalid email addresss entered."); 
                        }
                        else
                        {
                            System.out.println("Attempting mail server lookup...");
                            String domain = emailAddress.split("@")[1];
                            Attributes attributes = idc.getAttributes("dns:/" + domain, new String[] {"MX"});

                            if(attributes.size() == 0)
                            {
                                validServer = false;
                                System.out.println("No mail servers associated with that domain.");
                            }
                            else
                            {
                                validServer = true;
                                Attribute attributeMX = attributes.get("MX");

                                //Turns something like "5 smtp.exmaple.com" into "smtp.example.com"
                                MXServerName = attributeMX.get(0).toString().split(" ")[1];
                                System.out.println("Server: " + MXServerName);
                            }
                        }
                }
                catch(NamingException e)
                {
                    System.out.println("No DNS server found for server.");
                    System.out.println(e.getMessage());
                }
            }
        
        return MXServerName;      
    }
    
    private boolean validateEmail(String address)
    {
        //Only care if we can get a potential domain
        if(address.contains("@") && address.split("@").length == 2)
        {
            String domain = address.split("@")[1];
            return domain.contains(".");
        }
        else
        {
            return false;
        }
    }
    
    private void connectToServer(String serverName)
    {          
        try
        {
            MXServer = new Socket(serverName,25);
        }
        catch(UnknownHostException e)
        {
            System.out.println("Unable to connect to server.\n" + e.getMessage());
        }
        catch(IOException e)
        {
            System.out.println("IO Exception.\n" + e.getMessage());
        }

    }
    
    private void getStreams()
    {
       try
            {
                serverIn = new InputStreamReader(MXServer.getInputStream());
                serverOut = MXServer.getOutputStream();
                
                BufferedReader br = new BufferedReader(serverIn);
                System.out.println(br.readLine());

            }
            catch(IOException e)
            {
                System.out.println("IO Exception.\n" + e.getMessage());
            } 
    }
    
    private void processSpecifiedCommands()
    {
        System.out.println("Now accepting user commands. Enter 'QUIT' to exit.");
        Scanner sc = new Scanner(System.in);
        String command = " ";
        
        while(!command.equalsIgnoreCase("QUIT"))
        {
            System.out.print("Enter a command: ");
            command = sc.nextLine();
            passCommand(command);
        }

    }
    
    private void processCommands()
    {
        passCommand("HELO example.com"); //Need a better way of handling this to catch multi-line replies
        passCommand("MAIL FROM:<test@example.com>");
        passCommand("RCPT TO:<" + emailAddress + ">");
        passCommand("VRFY <" + emailAddress + ">");
        passCommand("QUIT");
    }
    
    private void passCommand(String cmd)
    {
        System.out.print(cmd + " ");
        try
        {
            String fullCommand = cmd + "\r\n";
            serverOut.write(fullCommand.getBytes("US-ASCII"));
            
            boolean moreBytes = true;

            while(moreBytes)
            {
                int serverByte = serverIn.read();
                System.out.print((char)serverByte);
                
                if(serverByte==13)
                {
                    serverByte = serverIn.read();
                    System.out.print((char)serverByte);
                    
                    if(serverByte==10)
                    {
                        moreBytes = false;
                    }
                    else
                    {
                       moreBytes = true;
                    }
                }

            }
            
            serverOut.flush();
        }
        catch(IOException e)
        {
            System.out.println(e.getMessage());
        }
        
        
            
    }
    
    private void cleanUp()
    {
        try
        {
            serverOut.close();
            serverIn.close();
            MXServer.close(); 
            System.exit(0);
        }
        catch(IOException e)
        {
            System.out.println("Unable to close resources");
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}


