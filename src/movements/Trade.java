package movements;


import main.ApiController;
import util.Utility;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;

import static movements.Option.STATE.BUYBACK;
import static movements.Option.STATE.OPEN;

/**
 * A trade is all option buys, sells and deliveries of one specific instrument combined
 */
public class Trade extends Movement {

    public ArrayList<Option> options;
    public Delivery delivery;

    public ApiController.CURRENCY currency;
    public String expiryDateStr;
    public Date expiryDate;
    public int strikePrice;
    public Option.KIND kind;
    public String instrumentName;
    public Option.STATE state;

    public String timeRemaining;
    public String initialRuntime;
    public double openPos;
    public double change;
    public double maxGain;
    public double timestamp;

    //evaluation
    public Double index;
    public Double diffToStrike;
    public Double ask;
    public Double avgPrem;
    public Double maxPrice;
    public Double priceDiff;
    public Double currPrice;
    public Double stopLoss;


    @Override
    public double getChange(){
        return change;
    }

    @Override
    public double getTimestamp(){
        return timestamp;
    }


    public Trade(ArrayList<Option> opts, Delivery del, Double index) throws Exception {
        options = opts;
        delivery = del;

        if(opts.size() == 0) throw new Exception("Optionlist for trade cannot be null!");

        Option op = opts.get(0);

        currency = op.currency;
        expiryDate = op.expiryDate;
        expiryDateStr = op.expiryDateStr;
        strikePrice = op.strikePrice;
        kind = op.kind;
        instrumentName = op.instrumentName;


        double opChange = 0.0;
        timestamp = op.timestamp;

        initialRuntime = convertMillisToHMmSs((expiryDate.getTime() - (long)timestamp));


        for(Option o : options){
            opChange += o.change;
        }
        maxGain = opChange;

        openPos = 0.0;
        if (del == null) {
            openPos = aggregatePositions(options);
            if(Utility.isZero(openPos, 0.0001)){
                state = BUYBACK;
            } else {
                state = OPEN;
            }
        } else {
            state = Option.STATE.FILLED;
            opChange += del.change;
        }


        timeRemaining = state == OPEN ? convertMillisToHMmSs(expiryDate.getTime() - System.currentTimeMillis()) : convertMillisToHMmSs(0);

        change = state == OPEN ? estimateValue(op, openPos, opChange, index, (expiryDate.getTime() - System.currentTimeMillis()), (expiryDate.getTime() - (long)timestamp)) : opChange;
    }

    private static Double estimateValue(Option op, double openPos, double change, double index, long timeRemaining, long initialRuntime){
        double diff = 0.0;

        if(op.kind == Option.KIND.PUT && op.strikePrice > index){
            diff = op.strikePrice - index;
        }
        if(op.kind == Option.KIND.CALL && op.strikePrice < index){
            diff = index - op.strikePrice;
        }
        double liability = diff * openPos;
        double effectiveDiff = liability + change * index;
        double btcEffDiff = effectiveDiff / index;

        double result = btcEffDiff;

        if(result > 0) {
            double percentRemaining = (double) timeRemaining / (double) initialRuntime;
            result = (1-percentRemaining) * btcEffDiff;
        }

        return result;
    }


    public static ArrayList<Trade> aggregateTrades(ArrayList<Option> options, ArrayList<Delivery> deliveries, ApiController.CURRENCY currency, Double index) throws Exception {
        ArrayList<Trade> results = new ArrayList<>();
        ArrayList<String> processed = new ArrayList<>();

        for(Option option : options){
            if(option.currency != currency) continue;
            if(processed.contains(option.instrumentName)) continue;
            ArrayList<Option> opts = new ArrayList<>();
            Delivery del = null;
            for(Option option2 : options){
                if(option.instrumentName.equals(option2.instrumentName)) opts.add(option2);
            }
            for(Delivery delivery : deliveries){
                if(option.instrumentName.equals(delivery.instrumentName)) del = delivery;
            }
            processed.add(option.instrumentName);
            results.add(new Trade(opts, del, index));
        }

        return results;
    }

    private double aggregatePositions(ArrayList<Option> options){
        double pos = 0.0;
        for(Option o : options){
            if(o.direction.equals("sell")) pos -= o.amount;
            if(o.direction.equals("buy")) pos += o.amount;
        }
        return pos;
    }

    private static String convertMillisToHMmSs(long ms) {
        long seconds = ms/1000;
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;
        long d = (seconds / (60 * 60))/24;
        return String.format("%02d:%02d:%02d:%02d",d, h,m,s);
    }
}
