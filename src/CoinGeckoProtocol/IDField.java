package CoinGeckoProtocol;

import java.util.Arrays;
import java.util.HashSet;

public enum IDField {
    NAME("Name: %s\n"),
    PLATFORM_ID("Asset Platform ID: %s\n"),
    PRICE("Price in USD: %f\n"),
    ALL("Name: %s\nAsset Platform ID: %s\nPrice in USD: %f\n");

    private String strToFormat;

    IDField(String strToFormat){
        this.strToFormat = strToFormat;
    }
    public String getStrToFormat(){
        return this.strToFormat;
    }
    public static IDField[] getUniqueIds(IDField[] idFields){
        HashSet<IDField> uniqueFields = new HashSet<>(Arrays.asList(idFields));
        return (uniqueFields.contains(ALL)) ? new IDField[]{ALL} : uniqueFields.toArray(new IDField[0]);
    }






}
