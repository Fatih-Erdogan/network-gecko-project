package Client;

import CoinGeckoProtocol.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Scanner;

public class GeckoClient {
    private static final String INFORMATIVE_MSG = "Type 'list' to get the whole list of NFTs,\n" +
                    "Use the following fields with a space in between as <NFT id> 'field'+\n" +
                    "\t'name' to get the name of the NFT" +
                    "\t'platform_id' to get the asset platform id of the NFT,\n" +
                    "\t'price' to get the floor price of the NFT in USD,\n" +
                    "\t'all' to get all 3 information,\n" +
                    "Type 'exit' to exit:\n";
    private static final Integer HEART_BEAT_PERIOD = 3000; // ms
    public static void main(String[] args){
        Scanner inp = new Scanner(System.in);
        String temporaryString;
        int temporaryInt;
        String serverAddress = ConnectionGeckoServer.DEFAULT_SERVER_ADDRESS;
        int serverPort = ConnectionGeckoServer.DEFAULT_SERVER_PORT;

        System.out.println("Server address (leave blank for default): ");
        temporaryString = inp.nextLine();
        if (!temporaryString.trim().equals("")){
            serverAddress = temporaryString.trim();
        }
        System.out.println("Server port (leave blank for default): ");
        temporaryString = inp.nextLine();
        if (!temporaryString.trim().equals("")){
            temporaryInt = Integer.parseInt(temporaryString.trim());
            serverPort = temporaryInt;
        }

        ConnectionGeckoServer connectionGeckoServer = new ConnectionGeckoServer(serverAddress, serverPort, HEART_BEAT_PERIOD);
        connectionGeckoServer.connect();

        System.out.println(INFORMATIVE_MSG);
        temporaryString = inp.nextLine().trim();
        while (connectionGeckoServer.isConnected()){

            CGPRequest request = CreateRequest(temporaryString);
            if (request == null){
                System.out.println("Invalid request!");
            }
            else{
                CGPResponse response = connectionGeckoServer.postRequest(request);
                // also handles null responses
                HandleResponse(response, connectionGeckoServer);
            }

            if (connectionGeckoServer.isConnected()) {
                System.out.println(INFORMATIVE_MSG);
                temporaryString = inp.nextLine().trim();
            }
        }

        System.out.println("Closing the socket...");
        if (connectionGeckoServer.isConnected()) {
            connectionGeckoServer.disconnect();
        }
        System.out.println("Socket successfully closed!");

    }


    private static CGPRequest CreateRequest(String reqString){
        if (reqString.length() == 0){
            return null;
        }
        if (reqString.equalsIgnoreCase("list")){
            return new CGPRequest(RequestType.LIST, BodyFormat.STRING, "", new IDField[]{});
        }
        else if (reqString.equalsIgnoreCase("exit")) {
            return new CGPRequest(RequestType.TERMINATE, BodyFormat.STRING, " ", new IDField[]{});
        }
        else{
            String[] idAndFields = reqString.split(" ");
            // it needs at least an id and a field
            if (idAndFields.length < 2){
                return null;
            }
            String nftId = idAndFields[0];
            IDField[] fields = new IDField[idAndFields.length - 1];
            for (int i = 1; i < idAndFields.length; i++){
                fields[i - 1] = IDField.valueOf(idAndFields[i].toUpperCase(Locale.ENGLISH));
            }

            return new CGPRequest(RequestType.ID, BodyFormat.STRING, nftId, fields);
        }
    }


    private static void HandleResponse(CGPResponse response, ConnectionGeckoServer connectionServer){
        if (response == null){
            connectionServer.disconnect();
            return;
        }
        BodyFormat responseFormat = response.getBodyFormat();

        switch (response.getResponseType()){
            case MAX_LIMIT_EXCEEDED:
                System.out.println("Reached to maximum request limit! Try again later.\n");
                break;
            case TIMEOUT:
                System.out.println("Timeout! Server was disconnected.\n");
                connectionServer.disconnect();
                break;
            case ACK_NOT_RECEIVED:
                System.out.println("Server didn't received ACK message! Disconnected");
                connectionServer.disconnect();
                break;
            // in case of failure, the string sent by the client is returned
            case FAILURE:
                System.out.println("Failure! Message:");
                System.out.println(response.getBody() + "\n");
                break;
            case SUCCESS:
                if (responseFormat == BodyFormat.STRING){
                    System.out.println("Server response: ");
                    System.out.println(response.getBody() + "\n");
                }
                else if (responseFormat == BodyFormat.JSON){
                    JSONObject jsonObject = new JSONObject(response.getBody());
                    String nftInfo = GetNftInfo(jsonObject);
                    System.out.println(nftInfo);
                }
                else if (responseFormat == BodyFormat.JSON_ARRAY){
                    JSONArray jsonArray = new JSONArray(response.getBody());
                    System.out.println("Following NFTs are found:\n");
                    for (int i = 0; i < jsonArray.length(); i++)
                    {
                        String nftInfo = GetNftInfo(jsonArray.getJSONObject(i));
                        System.out.println(nftInfo + "\n");
                    }
                }
                else{
                    assert false: "Unsupported body format!";
                }
                break;
        }
    }

    private static String GetNftInfo(JSONObject nftObject){
        String name = nftObject.getString("name");
        String nftInfo = "Information for " + name + ":\n";

        for (String key: nftObject.keySet()){
            if (key.equalsIgnoreCase("name")){
                continue;
            }
            else{
                nftInfo += key + ": " + nftObject.get(key) + "\n";
            }
        }
        return nftInfo.trim();
    }

}
