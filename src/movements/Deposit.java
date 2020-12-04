package movements;

import com.google.gson.internal.LinkedTreeMap;
import main.ApiController;
import main.Test2;

import java.util.ArrayList;

import static movements.Option.getIfNotNull;

public class Deposit extends Movement{

    public double updated_timestamp;
    public String transaction_id;
    public String state;
    public double received_timestamp;
    public ApiController.CURRENCY currency;
    public double amount;
    public String address;

    public String user;


    @Override
    public double getChange(){
        return amount;
    }

    @Override
    public double getTimestamp(){
        return received_timestamp;
    }

    @Override
    public String getUser(){
        return user;
    }

    public static ArrayList<Deposit> parseList(ArrayList<LinkedTreeMap> list){
        ArrayList<Deposit> results = new ArrayList<>();
        for(LinkedTreeMap map : list){
            Deposit dep = new Deposit();
            dep.updated_timestamp = (double) getIfNotNull(map, "updated_timestamp");
            dep.transaction_id = getIfNotNull(map, "transaction_id").toString();
            dep.state = getIfNotNull(map, "state").toString();
            dep.received_timestamp = (double) getIfNotNull(map, "received_timestamp");
            dep.currency = ApiController.CURRENCY.valueOf(getIfNotNull(map, "currency").toString());
            dep.amount = (double) getIfNotNull(map, "amount");
            dep.address = getIfNotNull(map, "address").toString();
            dep.user = getUser(dep.transaction_id, Test2.getUserMappingByCurrency(dep.currency));

            results.add(dep);
        }
        return results;
    }
}
