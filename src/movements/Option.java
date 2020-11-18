package movements;

import com.google.gson.internal.LinkedTreeMap;
import main.ApiController;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Option extends Movement {

    public ApiController.CURRENCY currency;
    public String expiryDateStr;
    public Date expiryDate;
    public int strikePrice;
    public KIND kind;
    public String instrumentName;
    public STATE state;

    public double underlying_price;
    public double trade_seq;
    public String trade_id;
    public double timestamp;
    public double tick_direction;
    public String profit_loss;
    public double price;
    public String order_type;
    public String order_id;
    public double mark_price;
    public double iv;
    public double index_price;
    public double fee;
    public String direction;
    public double amount;
    public double change;

    public static ArrayList<String> displayFields = new ArrayList<>();

    public String getCurrency() {
        return currency.toString();
    }
    public String getExpiryDateStr(){
        return expiryDateStr;
    }
    public String getExpiryDate() {
        return expiryDate.toString();
    }
    public String getStrikePrice() {
        return strikePrice + "";
    }
    public String getKind(){
        return kind.toString();
    }
    public String getInstrumentName(){
        return instrumentName;
    }
    public String getState(){
        return state.toString();
    }


    public enum KIND{
        CALL,
        PUT
    }

    public enum STATE{
        OPEN,
        BUYBACK,
        FILLED
    }

    public Option(String name){
        parseInstrumentName(name);
    }

    @Override
    public double getChange(){
        return change;
    }

    @Override
    public double getTimestamp(){
        return timestamp;
    }

    public void parseInstrumentName(String name){
        try{
            instrumentName = name;
            String[] tmp = name.split("-");
            currency = ApiController.CURRENCY.valueOf(tmp[0]);
            expiryDateStr = tmp[1];
            DateFormat format = new SimpleDateFormat("ddMMMyyHH:mm z", Locale.ENGLISH);
            expiryDate = format.parse(tmp[1] + "08:00 UTC");
            strikePrice = Integer.parseInt(tmp[2]);
            String abbrv = tmp[3].substring(0, 1);
            if(abbrv.equals("C")){
                kind = KIND.CALL;
            } else if(abbrv.equals("P")){
                kind = KIND.PUT;
            }
            if(expiryDate.before(Calendar.getInstance().getTime())){
                state = STATE.FILLED;
            } else {
                state = STATE.OPEN;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static ArrayList<Option> parseList(ArrayList<LinkedTreeMap> list){
        ArrayList<Option> result = new ArrayList<>();
        for(LinkedTreeMap map : list){
            Option option = new Option(getIfNotNull(map, "instrument_name").toString());
            option.underlying_price = (double)getIfNotNull(map, "underlying_price");
            option.trade_seq = (double)getIfNotNull(map, "trade_seq");
            option.trade_id = getIfNotNull(map, "trade_id").toString();
            option.timestamp = (double)getIfNotNull(map, "timestamp");
            option.tick_direction = (double)getIfNotNull(map, "tick_direction");
            option.profit_loss = getIfNotNull(map, "profit_loss").toString();
            option.price = (double)getIfNotNull(map, "price");
            option.order_type = getIfNotNull(map, "order_type").toString();
            option.order_id = getIfNotNull(map, "order_id").toString();
            option.mark_price = (double)getIfNotNull(map, "mark_price");
            option.iv = (double)getIfNotNull(map, "iv");
            option.index_price = (double)getIfNotNull(map, "index_price");
            option.fee = (double)getIfNotNull(map, "fee");
            option.direction = getIfNotNull(map, "direction").toString();
            option.amount = (double)getIfNotNull(map, "amount");
            if(option.direction.equals("sell")){
                option.change = option.price * option.amount - option.fee;
            } else if(option.direction.equals("buy")){
                option.change = option.price * option.amount * -1 - option.fee;
            }
            result.add(option);
        }
        return result;
    }

    public static Object getIfNotNull(LinkedTreeMap map, String key){
        Object obj = map.get(key);
        if(obj == null) obj = " ";
        return obj;
    }
}
