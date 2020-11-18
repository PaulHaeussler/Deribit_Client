package main;


import movements.*;
import server.ServerHandler;
import util.Printer;
import util.Utility;

import java.util.*;

public class Main {

    public static String usdToEur = "";

    private static ServerHandler sh;
    private static TimerTask tt;
    private static Timer t;

    private static ApiController api;

    public static HashMap<String, String> userMappings;

    public static void main(String[] args){

        try{
            Printer.checkSetup();
            Utility.checkStartupArgs(args);

            api = new ApiController();
            api.authenticate(new Credentials("C:\\Users\\Paul\\IdeaProjects\\Deribit Client\\src\\secrets\\api.key"));
            userMappings = Movement.getUserMappings("C:\\Users\\Paul\\IdeaProjects\\Deribit Client\\src\\secrets\\users_eth.mapping");

            System.out.println("Total trade history change: " + totalChange() + " BTC");
            System.out.println("Total trade history gains: " + totalGains() + " BTC");

            compileTradeList(true, ApiController.CURRENCY.ETH);

            //t = new Timer();
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

    private static void compileTradeList(boolean includeOpen, ApiController.CURRENCY currency) throws Exception {
        ArrayList<Option> options = Option.parseList(api.getTradeHistory(currency, ApiController.INSTRUMENT.option));
        ArrayList<Delivery> deliveries = Delivery.parseList(api.getSettlementHistory(currency, ApiController.SETTLEMENT.delivery));
        ArrayList<Deposit> deposits = Deposit.parseList(api.getDepositHistory(currency));
        ArrayList<Withdrawal> withdrawals = Withdrawal.parseList(api.getWithdrawalHistory(currency));

        ArrayList<Trade> trades = Trade.aggregateTrades(options, deliveries, currency);
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

        System.out.println("compiled");
    }
}
