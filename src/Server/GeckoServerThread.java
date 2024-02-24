package Server;

import CoinGecko.CoinGeckoAdapter;
import CoinGecko.GeckoAPIException;
import CoinGeckoProtocol.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;

import java.net.SocketTimeoutException;

public class GeckoServerThread implements Runnable {
    private Socket socket;
    private BufferedReader socketReader;
    private PrintWriter socketWriter;
    private final CoinGeckoAdapter geckoAdapter;
    private final int TIMEOUT;
    private static final int MAX_NUM_RESEND = 3;
    private static final int WAIT_DURATION_PER_SEND = 2000;


    public GeckoServerThread(Socket socket, int timeOut){
        this.socket = socket;
        this.TIMEOUT = timeOut;
        this.geckoAdapter = CoinGeckoAdapter.getInstance();
        try{
            this.socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.socketWriter = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(){
        CGPRequest request;
        String tempString;
        boolean control = true;
        try {
            while (control) {
                tempString = this.socketReader.readLine();
                request = CGPMessage.ParseRequest(tempString);
                control = this.handleRequest(request);
            }
        } catch (SocketTimeoutException e) {
            this.handleTimeoutSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean handleRequest(CGPRequest request){
        if (request == null){
            System.out.println("Client " + this.socket.getRemoteSocketAddress() + " left!");
            this.disconnect();
            return false;
        }
        RequestType type = request.getRequestType();
        CGPResponse lastResponse;
        switch (type) {
            case HEARTBEAT -> System.out.println("Heartbeat from: " + this.socket.getRemoteSocketAddress());
            case ACK -> System.out.println("Received unexpected ACK from: " + this.socket.getRemoteSocketAddress());
            case TERMINATE -> {
                System.out.println("Client is terminating!");
                this.disconnect();
                return false;
            }
            case LIST -> {
                try {
                    JSONArray nftList = geckoAdapter.queryList();
                    lastResponse = new CGPResponse(ResponseType.SUCCESS, BodyFormat.JSON_ARRAY, nftList.toString());
                    this.socketWriter.println(lastResponse);
                } catch (GeckoAPIException apiException) {
                    lastResponse = handleAPIException(apiException);
                }
                return this.waitForAck(lastResponse);
            }
            case ID -> {
                try {
                    JSONObject nft = this.geckoAdapter.queryNFT(request.getBody());
                    IDField[] idFields = IDField.getUniqueIds(request.getIdFields());
                    String body = this.createIdResponseBody(nft, idFields);

                    lastResponse = new CGPResponse(ResponseType.SUCCESS, BodyFormat.STRING, body);
                    this.socketWriter.println(lastResponse);
                } catch (GeckoAPIException apiException) {
                    lastResponse = handleAPIException(apiException);
                }
                return this.waitForAck(lastResponse);
            }
        }
        return true;
    }

    private String createIdResponseBody(JSONObject nft, IDField[] idFields) {
        String body = "";
        for (IDField idField : idFields){
            String toFormat = idField.getStrToFormat();
            switch (idField) {
                case NAME -> {
                    String name = nft.getString("name");
                    toFormat = String.format(toFormat, name);
                    body += toFormat;
                }
                case PLATFORM_ID -> {
                    String platform_id = nft.getString("asset_platform_id");
                    toFormat = String.format(toFormat, platform_id);
                    body += toFormat;
                }
                case PRICE -> {
                    float price = ((BigDecimal) nft.getJSONObject("floor_price").get("usd")).floatValue();
                    toFormat = String.format(toFormat, price);
                    body += toFormat;
                }
                case ALL -> {
                    String name = nft.getString("name");
                    String platform_id = nft.getString("asset_platform_id");
                    float price = ((BigDecimal) nft.getJSONObject("floor_price").get("usd")).floatValue();
                    toFormat = String.format(toFormat, name, platform_id, price);
                    body += toFormat;
                }
            }
        }
        return body;
    }

    private CGPResponse handleAPIException(GeckoAPIException apiException){
        ResponseType type = null;
        switch (apiException.getResponseCode()){
            case 404:
                type = ResponseType.FAILURE;
                break;
            case 429:
                type = ResponseType.MAX_LIMIT_EXCEEDED;
                break;
            default:
                System.out.println(apiException.getResponseCode());
                break;
        }
        CGPResponse response = new CGPResponse(type, BodyFormat.STRING, apiException.getMessage());
        this.socketWriter.println(response);
        return response;
    }

    private boolean waitForAck(CGPResponse lastResponse){
        boolean success = false;
        System.out.println("Waiting for ACK from: " + this.socket.getRemoteSocketAddress());

        for (int i = 0; i < MAX_NUM_RESEND + 1; i++){
            try {
                this.socket.setSoTimeout(WAIT_DURATION_PER_SEND);
                CGPRequest ack = CGPMessage.ParseRequest(this.socketReader.readLine());
                if (ack.getRequestType() == RequestType.ACK){
                    System.out.println("Received ACK from: " + this.socket.getRemoteSocketAddress());
                    success = true;
                    break;
                }
                else{
                    continue;
                }
            } catch (SocketTimeoutException e) {
                if (i != MAX_NUM_RESEND) {
                    this.socketWriter.println(lastResponse);
                }
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        }
        if (!success){
            this.handleAckNotReceived();
        }
        else{
            try {
                this.socket.setSoTimeout(this.TIMEOUT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return success;
    }
    private void handleAckNotReceived(){
        System.out.println("Acknowledgement didn't received! Closing socket.");
        this.socketWriter.println(new CGPResponse(ResponseType.ACK_NOT_RECEIVED, BodyFormat.STRING, " "));
        System.out.println("Informative message sent.");
        this.disconnect();
    }
    private void handleTimeoutSocket(){
        System.out.println("Client from " + this.socket.getRemoteSocketAddress().toString() + " timed out, closing socket...");
        this.socketWriter.println(new CGPResponse(ResponseType.TIMEOUT, BodyFormat.STRING, " "));
        this.disconnect();
        System.out.println("Socket closed!");
    }

    private void disconnect(){
        try{
            System.out.println("Disconnecting...");
            this.socketReader.close();
            this.socketWriter.flush();
            this.socketWriter.close();
            this.socket.close();
            System.out.println("Disconnected.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
