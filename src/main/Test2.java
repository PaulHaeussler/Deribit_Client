package main;


import com.google.gson.internal.LinkedTreeMap;
import movements.*;
import server.ServerHandler;
import util.Printer;
import util.Utility;

import java.util.*;

public class Test2 {

    public static String repo = "";

    private static ApiController api;

    public static HashMap<String, String> userMappingsBTC;
    public static HashMap<String, String> userMappingsETH;

    public static void main(String[] args){

        try{
            Printer.checkSetup();
            Utility.checkStartupArgs(args);

            api = new ApiController();
            api.authenticate(new Credentials(repo + "\\api.key"));
            userMappingsBTC = Movement.getUserMappings(repo + "\\users_btc.mapping");
            userMappingsETH = Movement.getUserMappings(repo + "\\users_eth.mapping");

            System.out.println("Total trade history change: " + totalChange() + " BTC");
            System.out.println("Total trade history gains: " + totalGains() + " BTC");



            TreeMap<Double, Moment> hb = compileTradeList(true, ApiController.CURRENCY.BTC);
            TreeMap<Double, Moment> he = compileTradeList(true, ApiController.CURRENCY.ETH);

            LinkedTreeMap map = api.getBookSummary("BTC-11DEC20-20000-C");
            System.out.println();
            //t.scheduleAtFixedRate(tt, 5000, 5000);
            //while(true);

        } catch (Exception e){
            Printer.printException(e);
        }
    }


    private static double totalGains() throws Exception {
        ArrayList<Option> options = Option.parseList(api.getTradeHistory(ApiController.CURRENCY.BTC, ApiController.INSTRUMENT.option));
        double change = 0.0;

        for(Option option : options){
            if(option.change > 0.0) change += option.change;
        }


        return change;
    }

    private static double totalChange() throws Exception {
        ArrayList<Option> options = Option.parseList(api.getTradeHistory(ApiController.CURRENCY.BTC, ApiController.INSTRUMENT.option));
        ArrayList<Delivery> deliveries = Delivery.parseList(api.getSettlementHistory(ApiController.CURRENCY.BTC, ApiController.SETTLEMENT.delivery));
        double change = 0.0;

        for(Option option : options){
            change += option.change;
        }
        for(Delivery delivery : deliveries){
            change += delivery.change;
        }

        return change;
    }

    private static TreeMap<Double, Moment> compileTradeList(boolean includeOpen, ApiController.CURRENCY currency) throws Exception {
        ArrayList<Option> options = Option.parseList(api.getTradeHistory(currency, ApiController.INSTRUMENT.option));
        ArrayList<Delivery> deliveries = Delivery.parseList(api.getSettlementHistory(currency, ApiController.SETTLEMENT.delivery));
        ArrayList<Deposit> deposits = Deposit.parseList(api.getDepositHistory(currency));
        ArrayList<Withdrawal> withdrawals = Withdrawal.parseList(api.getWithdrawalHistory(currency));

        Double index = api.getIndex(currency);
        ArrayList<Trade> trades = Trade.aggregateTrades(options, deliveries, currency, index);
        TreeMap<Double, Movement> sortedMovements = new TreeMap<>();


        for(Deposit dep : deposits){
            System.out.println("Deposit " + dep.amount + " BTC ID=" + dep.transaction_id);
            sortedMovements.put(dep.getTimestamp(), dep);
        }

        for(Withdrawal wit : withdrawals){
            System.out.println("Withdrawal " + wit.amount + " BTC ID=" + wit.transaction_id);
            sortedMovements.put(wit.getTimestamp(), wit);
        }

        for(Trade trade : trades){
            System.out.println(trade.instrumentName + "     Change: " + trade.getChange());
            if((!includeOpen && trade.state != Option.STATE.OPEN) || includeOpen) sortedMovements.put(trade.getTimestamp(), trade);
        }

        TreeMap<Double, Moment> moments = new TreeMap<>();
        Moment prev = null;
        for(Movement mov : sortedMovements.values()){
            Moment moment = new Moment(mov, prev);
            prev = moment;
            moments.put(moment.timestamp, moment);
        }

        return moments;
    }


    public static HashMap<String, String> getUserMappingByCurrency(ApiController.CURRENCY currency){
        if(currency == ApiController.CURRENCY.BTC){
            return userMappingsBTC;
        } else if(currency == ApiController.CURRENCY.ETH) {
            return userMappingsETH;
        }
        return null;
    }
}
