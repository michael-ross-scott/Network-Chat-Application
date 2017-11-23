import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 *
 * @author sctmic012
 */

//Made this for ease of running
public class FirstClient
{
    public static void main(String[] args) throws UnknownHostException
    {
        String hostName = InetAddress.getLocalHost().getHostAddress();
        
        System.out.println("Enter a port number > 2000, then push enter");
        Scanner sc = new Scanner(System.in);
        int portNumber = sc.nextInt();
        
        Client first = new Client(portNumber, hostName);
    }
}
