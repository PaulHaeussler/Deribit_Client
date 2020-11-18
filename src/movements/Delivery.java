package movements;

import com.google.gson.internal.LinkedTreeMap;
import main.ApiController;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static movements.Option.getIfNotNull;

public class Delivery extends Movement {

    public static final double FEE_RATE = 0.00015;

    public String instrumentName;
    public ApiController.CURRENCY currency;
    public Date expiryDate;
    public Option.KIND kind;
    public String expiryDateStr;
    public int strikePrice;

    public ApiController.SETTLEMENT type;
    public double timestamp;
    public double sessionPNL;
    public double pnl;
    public double pos;
    public double markPrice;
    public double index_price;

    public double cashflow;
    public double feePaid;
    public double change;


    public Delivery(String name){
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
                kind = Option.KIND.CALL;
            } else if(abbrv.equals("P")){
                kind = Option.KIND.PUT;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static ArrayList<Delivery> parseList(ArrayList<LinkedTreeMap> list) {
        ArrayList<Delivery> result = new ArrayList<>();
        for(LinkedTreeMap map : list){
            Delivery dl = new Delivery(getIfNotNull(map, "instrument_name").toString());
            dl.type = ApiController.SETTLEMENT.valueOf(getIfNotNull(map, "type").toString());
            dl.timestamp = (double) getIfNotNull(map, "timestamp");
            dl.sessionPNL = (double) getIfNotNull(map, "session_profit_loss");
            dl.pnl = (double) getIfNotNull(map, "profit_loss");
            dl.pos = (double) getIfNotNull(map, "position");
            dl.markPrice = (double) getIfNotNull(map, "mark_price");
            dl.index_price = (double) getIfNotNull(map, "index_price");

            if((dl.kind == Option.KIND.CALL && dl.index_price > dl.strikePrice) || (dl.kind == Option.KIND.PUT && dl.index_price < dl.strikePrice)){
                dl.cashflow = dl.pos * dl.markPrice * -1.0;
                dl.feePaid = dl.pos * FEE_RATE;
                dl.change = dl.cashflow - dl.feePaid;
            } else {
                dl.feePaid = 0.0;
                dl.cashflow = 0.0;
                dl.change = 0.0;
            }
            result.add(dl);
        }

        return result;
    }

}
