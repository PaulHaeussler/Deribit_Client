package main;
import com.google.gson.internal.LinkedTreeMap;
import org.openapitools.client.*;

import java.util.*;

public class Test {

    public static void main(String[] args) {
        try{
            ApiController api = new ApiController();
            api.autheticate(new Credentials("C:\\Users\\Paul\\IdeaProjects\\Deribit Client\\src\\secrets\\api.key"));
            ArrayList<LinkedTreeMap> tradeList = api.getTradeHistory(ApiController.CURRENCY.BTC, ApiController.INSTRUMENT.option);
            ArrayList<LinkedTreeMap> depositList = api.getDepositHistory(ApiController.CURRENCY.BTC);
            ArrayList<LinkedTreeMap> withdrawalList = api.getWithdrawalHistory(ApiController.CURRENCY.BTC);
            ArrayList<LinkedTreeMap> deliveryList = api.getSettlementHistory(ApiController.CURRENCY.BTC, ApiController.SETTLEMENT.delivery);
            ArrayList<LinkedTreeMap> openOrders = api.getOpenOrders(ApiController.CURRENCY.BTC, ApiController.INSTRUMENT.option);

            ArrayList<String> headers = new ArrayList<>();
            headers.add("underlying_price");
            headers.add("timestamp");
            headers.add("profit_loss");
            headers.add("price");
            headers.add("mark_price");
            headers.add("index_price");
            headers.add("fee");

            printCSV(tradeList, 24, headers);
            System.out.printf("%.7f", api.getTotalInOut(depositList, withdrawalList));
            System.out.println();

            headers = new ArrayList<>();
            headers.add("session_profit_loss");
            headers.add("timestamp");
            headers.add("profit_loss");
            headers.add("mark_price");
            headers.add("index_price");

            printCSV(deliveryList, 8, headers);

            System.out.println("\nyay");
        } catch (ApiException e){
            e.printStackTrace();
            System.out.println(e.getResponseBody());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void printCSV(ArrayList<LinkedTreeMap> transactionList, int maxHeaderCount, ArrayList<String> headerToBeFormattedAsDoubles){
        int iterations = 0;
        boolean first = true;
        ArrayList<Object> keys = new ArrayList<>();
        for(LinkedTreeMap map : transactionList){
            if(map.size() == maxHeaderCount && first){
                for(Object key : map.keySet()){
                    if(iterations > 0) System.out.print(",");
                    System.out.print(key.toString());
                    keys.add(key);
                    iterations++;
                }
                System.out.println();
                first = false;
            }
        }
        for(LinkedTreeMap map : transactionList){
            iterations = 0;
            for(Object key : keys){
                iterations++;
                if(map.get(key) == null) {
                    System.out.print(" ");
                } else {
                    boolean match = false;
                    for(String str : headerToBeFormattedAsDoubles){
                        if(key.toString().equals(str)){
                            System.out.print(String.format("%.12f", (Double)map.get(key)).replace(",", "."));
                            match = true;
                            break;
                        }
                    }
                    if(!match) System.out.print(map.get(key).toString());
                }

                if(iterations < maxHeaderCount) System.out.print(",");
            }
            System.out.println();
        }

    }
}