package CoinGeckoProtocol;

public class CGPRequest extends CGPMessage {
    private RequestType reqType;
    private BodyFormat bodyFormat;
    private String body;
    private IDField[] idFields;
    public CGPRequest(RequestType reqType, BodyFormat bodyFormat, String body, IDField[] idFields) {
        if (reqType == RequestType.ID){
            assert (idFields.length > 0): "If you provide an NDF ID, you should also provide a field!";
        }
        this.reqType = reqType;
        this.bodyFormat = bodyFormat;
        this.body = body;
        this.idFields = idFields;
    }
    public RequestType getRequestType() {return this.reqType;}
    public String getBody() {return this.body;}
    public IDField[] getIdFields() {
        return this.idFields;
    }

    public BodyFormat getBodyFormat(){return this.bodyFormat;}

    @Override
    public String toString(){
        String message = this.reqType.toString() + DELIMITER +
                this.bodyFormat.toString() + DELIMITER;

        String replacedBody = this.body.replace("\n", NEWLINE_PLACEHOLDER);
        message += replacedBody;

        if (this.body.length() == 0){
            message += " ";
        }

        for (IDField field : this.idFields){
            message += DELIMITER + field.toString();
        }
        return message;
    }
}
