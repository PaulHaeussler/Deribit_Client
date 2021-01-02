package movements;


import com.google.gson.internal.LinkedTreeMap;
import main.ApiController;
import util.Printer;
import util.Utility;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;

import static main.Test.api;
import static movements.Option.STATE.BUYBACK;
import static movements.Option.STATE.OPEN;
import static util.Printer.LOGTYPE.INFO;

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
    public Double bid;
    public Double avgPrem;
    public Double maxPrice;
    public Double priceDiff;
    public Double currValue;
    public Double stopLoss;
    public Double valIfDelivery;


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
        if(openPos > 0){
            System.currentTimeMillis();
        }

        timeRemaining = state == OPEN ? convertMillisToHMmSs(expiryDate.getTime() - System.currentTimeMillis()) : convertMillisToHMmSs(0);
        change = opChange;
        change = state == OPEN ? estimateValue(op, openPos, opChange, index, (expiryDate.getTime() - System.currentTimeMillis()), (expiryDate.getTime() - (long)timestamp)) : opChange;
    }

    private Double estimateValue(Option op, double openPos, double change, double index, long timeRemaining, long initialRuntime){
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

        LinkedTreeMap map = null;
        try{
            this.index = api.getIndex(this.currency);
            map = api.getBookSummary(this.instrumentName);
        } catch (Exception e){
            Printer.printException(e);
            System.exit(1);
        }

        //pos diff == out of money; neg diff == in the money
        this.diffToStrike = 0.0;

        if(this.kind == Option.KIND.PUT){
            this.diffToStrike = this.index - this.strikePrice;
        }
        if(this.kind == Option.KIND.CALL){
            this.diffToStrike = this.strikePrice - this.index;
        }


        this.ask = (Double) map.get("ask_price");
        this.bid = (Double) map.get("bid_price");


        if(openPos < 0){
            this.avgPrem = Math.abs(this.maxGain / this.openPos);
            this.priceDiff = this.ask - this.avgPrem;
            this.currValue = this.priceDiff * this.openPos;
        } else {
            this.maxGain = 0.0;
            this.avgPrem = Math.abs(this.change / this.openPos);
            this.priceDiff = (this.bid - this.avgPrem) * this.openPos;
            this.valIfDelivery = this.diffToStrike * this.openPos;
            this.currValue = Math.max(this.priceDiff, this.valIfDelivery);
        }

        DecimalFormat df = new DecimalFormat("#.00");
        DecimalFormat df2 = new DecimalFormat("0.00000");
        Printer.printToLog(this.instrumentName + " diff to strike: " + df.format(this.diffToStrike) + "; PaidPrice: " + df2.format(this.change) + " - Bid: " + df2.format(this.bid) + "; CurrVal: " + df2.format(this.currValue), INFO);
        
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
