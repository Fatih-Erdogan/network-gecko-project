package CoinGecko;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CoinGeckoAdapter {
    private static CoinGeckoAdapter adapter;
    public final String LIST_ENDPOINT_STRING = "https://api.coingecko.com/api/v3/nfts/list";
    public final String NFT_ENDPOINT_STRING = "https://api.coingecko.com/api/v3/nfts/";

    private CoinGeckoAdapter(){}

    public static synchronized CoinGeckoAdapter getInstance(){
        if (CoinGeckoAdapter.adapter == null){
            CoinGeckoAdapter.adapter = new CoinGeckoAdapter();
        }
        return CoinGeckoAdapter.adapter;
    }
    public String getLIST_ENDPOINT() {
        return this.LIST_ENDPOINT_STRING;
    }
    public String getNFT_ENDPOINT() {
        return this.NFT_ENDPOINT_STRING;
    }

    public JSONArray queryList () throws GeckoAPIException {
        HttpURLConnection connection;
        URL listEndpoint;
        try {

            listEndpoint = new URL(this.LIST_ENDPOINT_STRING);
            connection = (HttpURLConnection) listEndpoint.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode()>= 400){
                this.handleAPIException(connection);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = "";
            String line;
            while ((line = reader.readLine()) != null) {
                response += line; // just 1 line in fact
            }

            return new JSONArray(response);

        } catch (MalformedURLException e) {
            System.out.println("Malformed URL exception in GeckoCGPAdapter");
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject queryNFT(String nftId) throws GeckoAPIException{
        URL nftEndpoint;
        HttpURLConnection connection;
        try {
            nftEndpoint = new URL(this.NFT_ENDPOINT_STRING + nftId);
            connection = (HttpURLConnection) nftEndpoint.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // If connection.getResponseCode() is not called,
            // then the output of connection.getErrorStream is null
            if (connection.getResponseCode()>= 400){
                // throws GeckoAPIException
                this.handleAPIException(connection);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = "";
            String line;
            while ((line = reader.readLine()) != null) {
                response += line; // just 1 line in fact
            }

            return new JSONObject(response);

        } catch (MalformedURLException e) {
            System.out.println("Malformed URL exception in GeckoCGPAdapter");
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println("Exception while trying to retrieve data from CoinGecko API");
            throw new RuntimeException(e);
        }
    }

    private void handleAPIException(HttpURLConnection connection) throws GeckoAPIException{

        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
            int responseCode = connection.getResponseCode();
            String errorString = "";
            String line;
            while ((line = errorReader.readLine()) != null){
                errorString += line;
            }

            JSONObject errorObject = new JSONObject(errorString);
            String errorMessage;
            switch (responseCode){
                case (404) -> errorMessage = (String) errorObject.get("error");
                case (429) -> errorMessage = (String) errorObject.getJSONObject("status").get("error_message");
                default -> errorMessage = null;
            }

            throw new GeckoAPIException(responseCode, errorMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
