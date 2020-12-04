package movements;

import com.google.gson.internal.LinkedTreeMap;
import main.ApiController;
import main.Test2;

import java.util.ArrayList;

import static movements.Option.getIfNotNull;

public class Withdrawal extends Movement{

    public double updated_timestamp;
    public String transaction_id;
    public String state;
    public double created_timestamp;
    public double confirmed_timestamp;
    public ApiController.CURRENCY currency;
    public double amount;
    public String address;
    public double id;
    public double fee;
    public double priority;

    public String user;

    @Override
    public double getChange(){
        return (amount + fee) * -1.0;
    }

    @Override
    public double getTimestamp(){
        return created_timestamp;
    }

    @Override
    public String getUser(){
        return user;
    }

    public static ArrayList<Withdrawal> parseList(ArrayList<LinkedTreeMap> list){
        ArrayList<Withdrawal> results = new ArrayList<>();
        for(LinkedTreeMap map : list){
            Withdrawal wit = new Withdrawal();
            wit.updated_timestamp = (double) getIfNotNull(map, "updated_timestamp");
            wit.transaction_id = getIfNotNull(map, "transaction_id").toString();
            wit.state = getIfNotNull(map, "state").toString();
            wit.created_timestamp = (double) getIfNotNull(map, "created_timestamp");
            wit.confirmed_timestamp = (double) getIfNotNull(map, "confirmed_timestamp");
            wit.currency = ApiController.CURRENCY.valueOf(getIfNotNull(map, "currency").toString());
            wit.amount = (double) getIfNotNull(map, "amount");
            wit.address = getIfNotNull(map, "address").toString();
            wit.id = (double) getIfNotNull(map, "id");
            wit.fee = (double) getIfNotNull(map, "fee");
            wit.priority = (double) getIfNotNull(map, "priority");
            wit.user = getUser(wit.transaction_id, Test2.getUserMappingByCurrency(wit.currency));

            results.add(wit);
        }
        return results;
    }
}
