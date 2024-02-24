package CoinGeckoProtocol;

public abstract class CGPMessage {
    protected static final String DELIMITER = "|||";
    protected static final String REGEXP_DELIMITER = "\\|\\|\\|";
    protected static final String NEWLINE_PLACEHOLDER = "<|NEWLINE|>";
    protected static final String REGEXP_NEWLINE_PLACEHOLDER = "<|NEWLINE|>";

    public static CGPResponse ParseResponse(String response){
        if (response == null){ return null; }
        String originalResponse= response.replace(REGEXP_NEWLINE_PLACEHOLDER, "\n");
        String[] fieldsList = originalResponse.split(REGEXP_DELIMITER);
        ResponseType type = ResponseType.valueOf(fieldsList[0]);
        BodyFormat format = BodyFormat.valueOf(fieldsList[1]);
        String body = (fieldsList.length >= 3) ? fieldsList[2]: "";
        return new CGPResponse(type, format, body);
    }

    public static CGPRequest ParseRequest(String request){
        if (request == null){ return null; }
        String originalResponse= request.replace(REGEXP_NEWLINE_PLACEHOLDER, "\n");
        String[] fieldsList = originalResponse.split(REGEXP_DELIMITER);
        RequestType type = RequestType.valueOf(fieldsList[0]);
        BodyFormat format = BodyFormat.valueOf(fieldsList[1]);
        String body = fieldsList[2];
        IDField[] idFields = new IDField[fieldsList.length - 3];
        for (int i = 3; i < fieldsList.length; i++){
            idFields[i - 3] = IDField.valueOf(fieldsList[i]);
        }
        return new CGPRequest(type, format, body, idFields);
    }

    @Override
    public abstract String toString();

}
