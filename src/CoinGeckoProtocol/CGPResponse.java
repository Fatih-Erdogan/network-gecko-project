package CoinGeckoProtocol;

public class CGPResponse extends CGPMessage {
    private ResponseType responseType;
    private BodyFormat bodyFormat;
    private String body;
    public CGPResponse(ResponseType responseType, BodyFormat bodyFormat, String body) {
        this.responseType = responseType;
        this.bodyFormat = bodyFormat;
        this.body = body;
    }

    public ResponseType getResponseType() {
        return responseType;
    }
    public BodyFormat getBodyFormat(){ return this.bodyFormat;}
    public String getBody() {
        return body;
    }

    @Override
    public String toString(){
        String message = this.responseType.toString() + DELIMITER + this.bodyFormat.toString() + DELIMITER;
        String replacedBody = this.body.replace("\n", NEWLINE_PLACEHOLDER);
        message += replacedBody;
        if (this.body.length() == 0){
            message += " ";
        }
        return message;
    }

}
