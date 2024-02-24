package Client;

import CoinGeckoProtocol.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectionGeckoServer {
    public static final String DEFAULT_SERVER_ADDRESS = "localhost";
    public static final int DEFAULT_SERVER_PORT = 30416;

    private final Integer HeartbeatPeriod;
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private BufferedReader socketReader;
    private PrintWriter socketWriter;
    private boolean connected = false;

    public ConnectionGeckoServer(String serverAddress, int serverPort, Integer heartBeatPeriod){
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.HeartbeatPeriod = heartBeatPeriod;
    }

    public void connect(){
        try{
            this.socket = new Socket(this.serverAddress, this.serverPort);
            this.socketReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.socketWriter = new PrintWriter(this.socket.getOutputStream(), true);
            System.out.println("Connected to the server at: " + this.socket.getRemoteSocketAddress() + "\n");
            this.connected = true;
        } catch (UnknownHostException e) {
            System.err.println("No host found at" + this.serverAddress + "/" + this.serverPort);
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (this.HeartbeatPeriod != null){
            startHeartbeatTimer(this.HeartbeatPeriod);
        }
    }

    public CGPResponse postRequest(CGPRequest request){

        try {
            String response;
            while (this.socketReader.ready()){
                response = this.socketReader.readLine();
                if (response == null){
                    // disconnection is handled later
                    System.out.println("Server was disconnected, possibly because it couldn't received ACK");
                    return null;
                }
                CGPResponse protocolResponse = CGPMessage.ParseResponse(response);
                if (protocolResponse.getResponseType() == ResponseType.TIMEOUT ||
                        protocolResponse.getResponseType() == ResponseType.ACK_NOT_RECEIVED)
                    return protocolResponse;
            }

            this.socketWriter.println(request.toString());
            response = this.socketReader.readLine();
            this.socketWriter.println(new CGPRequest(RequestType.ACK, BodyFormat.STRING, "", new IDField[]{}));
            return CGPMessage.ParseResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void startHeartbeatTimer(Integer period) {
        Timer heartbeatTimer = new Timer(true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendHeartbeat();
                }
                catch (Exception e) {
                    this.cancel();
                    System.out.println("Failed to send heartbeat");
                    System.err.println("Failed to send heartbeat");
                    e.printStackTrace();
                }
            }
        }, 0, period);  // initial delay, period
    }

    private void sendHeartbeat() {
        CGPRequest heartbeatRequest = new CGPRequest(RequestType.HEARTBEAT, BodyFormat.STRING, "", new IDField[]{});
        socketWriter.println(heartbeatRequest);
    }

    public void disconnect(){
        this.connected = false;
        try {
            this.socketReader.close();
            this.socketWriter.close();
            this.socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isConnected(){
        return this.connected;
    }
}
