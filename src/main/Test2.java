package main;


import com.google.gson.internal.LinkedTreeMap;
import db.Database;
import movements.*;
import server.ServerHandler;
import util.Printer;
import util.Utility;

import java.util.*;

public class Test2 {

    public static String repo = "";

    private static ApiController api;



    public static void main(String[] args){

        try{
            Printer.checkSetup("DeribitClient");
            HashMap<String, String> argmap = Utility.checkStartupArgs(args);
            repo = argmap.get("repo");
            Database db = new Database(argmap.get("user"), argmap.get("pw"), argmap.get("host"), argmap.get("dbname"));



            api = new ApiController();
            api.authenticate(new Credentials(repo + "/api.key"));
            ApiController.userMappingsBTC = Movement.getUserMappings(repo + "/users_btc.mapping");
            ApiController.userMappingsETH = Movement.getUserMappings(repo + "/users_eth.mapping");

            System.out.println("Total trade history change: " + totalChange() + " BTC");
            System.out.println("Total trade history gains: " + totalGains() + " BTC");



            TreeMap<Double, Moment> hb = ApiController.compileTradeList(api, true, ApiController.CURRENCY.BTC);
            TreeMap<Double, Moment> he = ApiController.compileTradeList(api, true, ApiController.CURRENCY.ETH);

            //LinkedTreeMap map = api.getBookSummary("BTC-11DEC20-20000-C");
            ArrayList<LinkedTreeMap> m = api.getInstruments(ApiController.CURRENCY.BTC, false);
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
}
