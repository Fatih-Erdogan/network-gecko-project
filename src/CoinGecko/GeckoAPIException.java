package CoinGecko;

public class GeckoAPIException extends Exception{
    private int responseCode;

    public GeckoAPIException(int statusCode, String message) {
        super(message);
        this.responseCode = statusCode;
    }

    public int getResponseCode() {
        return this.responseCode;
    }
}
