package Server;

public class RunServer {
    public static final String SERVER_ADDRESS = "0.0.0.0";
    public static final int SERVER_PORT = 30416;
    public static final int TIMEOUT = 8000;

    public static void main(String[] args){
        GeckoServer server = new GeckoServer(SERVER_PORT, SERVER_ADDRESS, TIMEOUT);
        server.run();
    }
}
