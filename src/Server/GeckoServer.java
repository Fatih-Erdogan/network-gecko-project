package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;

public class GeckoServer {

    private final ServerSocket SERVER_SOCKET;
    private final int TIMEOUT;
    public GeckoServer(int port, String address, int timeOut) {

        try
        {
            this.SERVER_SOCKET = new ServerSocket(port, 0, InetAddress.getByName(address));
            System.out.println("Opened up a server socket on " + this.SERVER_SOCKET.getLocalSocketAddress());
            this.TIMEOUT = timeOut;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("Server class.Constructor exception on opening a server socket");
            throw new RuntimeException();
        }
    }

    public void run(){
        while (true){
            this.listenAndAccept();
        }
    }

    public void listenAndAccept() {
        try {
            Socket socket = this.SERVER_SOCKET.accept();
            displayClient(socket);
            socket.setSoTimeout(this.TIMEOUT);
            GeckoServerThread clientThread = new GeckoServerThread(socket, TIMEOUT);
            Thread t = new Thread(clientThread);
            t.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void displayClient(Socket socket){
        System.out.println("Got a connection from client at: " + socket.getRemoteSocketAddress().toString());
    }
}
