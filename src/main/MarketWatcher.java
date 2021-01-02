package main;

import com.google.gson.internal.LinkedTreeMap;
import db.Database;
import movements.Moment;
import movements.Movement;
import util.Printer;
import util.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class MarketWatcher {



    public static String repo = "";

    private static ApiController api;



    public static void main(String[] args){

        try{
            Printer.checkSetup("DeribitMarket");
            HashMap<String, String> argmap = Utility.checkStartupArgs(args);
            repo = argmap.get("repo");
            Database db = new Database(argmap.get("user"), argmap.get("pw"), argmap.get("host"), argmap.get("dbname"));


            api = new ApiController();
            api.authenticate(new Credentials(repo + "/api.key"));
            ApiController.userMappingsBTC = Movement.getUserMappings(repo + "/users_btc.mapping");
            ApiController.userMappingsETH = Movement.getUserMappings(repo + "/users_eth.mapping");



            ArrayList<LinkedTreeMap> m = api.getInstruments(ApiController.CURRENCY.BTC, false);
            System.out.println();

        } catch (Exception e){
            Printer.printException(e);
        }
    }
}
