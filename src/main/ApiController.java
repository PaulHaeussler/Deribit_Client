package main;

import com.google.gson.internal.LinkedTreeMap;
import movements.*;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.api.*;
import org.openapitools.client.auth.HttpBasicAuth;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class ApiController {

    private ApiClient defaultClient;

    private PrivateApi privateApi;
    private TradingApi tradingApi;
    private AccountManagementApi accountManagementApi;
    private WalletApi walletApi;
    private MarketDataApi marketDataApi;


    public static HashMap<String, String> userMappingsBTC;
    public static HashMap<String, String> userMappingsETH;

    public enum CURRENCY{
        BTC,
        ETH
    }

    public enum INSTRUMENT {
        option,
        future
    }

    public enum SETTLEMENT {
        settlement,
        delivery,
        bankruptcy
    }

    public ApiController(){
        defaultClient = Configuration.getDefaultApiClient();
    }

    public boolean authenticate(Credentials creds){
        try{
            HttpBasicAuth bearerAuth = (HttpBasicAuth) defaultClient.getAuthentication("bearerAuth");
            bearerAuth.setUsername(creds.getUsername());
            bearerAuth.setPassword(creds.getSecret());

            privateApi = new PrivateApi();
            marketDataApi = new MarketDataApi();
            tradingApi = new TradingApi();
            accountManagementApi = new AccountManagementApi();
            walletApi = new WalletApi();
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Double getEquity(CURRENCY type) throws Exception {
        LinkedTreeMap tmp1 = (LinkedTreeMap) accountManagementApi.privateGetAccountSummaryGet(type.toString(), true);
        LinkedTreeMap tmp2 = (LinkedTreeMap) tmp1.get("result");
        return (Double) tmp2.get("equity");
    }

    public Double getIndex(CURRENCY type) throws Exception {
        LinkedTreeMap tmp1 = (LinkedTreeMap) marketDataApi.publicGetIndexGet(type.toString());
        LinkedTreeMap tmp2 = (LinkedTreeMap) tmp1.get("result");
        return (Double) tmp2.get(type.toString());
    }


    public ArrayList<LinkedTreeMap> getTradeHistory(CURRENCY type, INSTRUMENT instrument) throws Exception {
        //1000 entries is maximum
        LinkedTreeMap tmp1 = (LinkedTreeMap) tradingApi.privateGetUserTradesByCurrencyGet(
                type.toString(), instrument.toString(), null, null, 1000,  true, "default");
        LinkedTreeMap tmp2 = (LinkedTreeMap) tmp1.get("result");
        if((boolean)tmp2.get("has_more")) throw new Exception("Didnt retrieve complete list, theres more!");
        return (ArrayList<LinkedTreeMap>) tmp2.get("trades");
    }

    public ArrayList<LinkedTreeMap> getDepositHistory(CURRENCY type) throws ApiException{
        ArrayList<LinkedTreeMap> result = new ArrayList<>();
        LinkedTreeMap tmp1 = (LinkedTreeMap) walletApi.privateGetDepositsGet(type.toString(), 1000, 0);
        LinkedTreeMap tmp2 = (LinkedTreeMap) tmp1.get("result");
        return (ArrayList<LinkedTreeMap>) tmp2.get("data");
    }

    public ArrayList<LinkedTreeMap> getWithdrawalHistory(CURRENCY type) throws ApiException{
        ArrayList<LinkedTreeMap> result = new ArrayList<>();
        LinkedTreeMap tmp1 = (LinkedTreeMap) walletApi.privateGetWithdrawalsGet(type.toString(), 1000, 0);
        LinkedTreeMap tmp2 = (LinkedTreeMap) tmp1.get("result");
        return (ArrayList<LinkedTreeMap>) tmp2.get("data");
    }

    public ArrayList<LinkedTreeMap> getSettlementHistory(CURRENCY type, SETTLEMENT settlementType) throws Exception {
        //1000 entries is maximum
        LinkedTreeMap tmp1 = (LinkedTreeMap) tradingApi.privateGetSettlementHistoryByCurrencyGet(
                type.toString(), settlementType.toString(), 1000);
        LinkedTreeMap tmp2 = (LinkedTreeMap) tmp1.get("result");
        if (!tmp2.get("continuation").equals("none")) throw new Exception("Didnt retrieve complete list, theres more!");
        return (ArrayList<LinkedTreeMap>) tmp2.get("settlements");
    }

    public ArrayList<LinkedTreeMap> getOpenOrders(CURRENCY type, INSTRUMENT instrument) throws Exception {
        //1000 entries is maximum
        LinkedTreeMap tmp1 = (LinkedTreeMap) tradingApi.privateGetOpenOrdersByCurrencyGet(
                type.toString(), instrument.toString(), "all");
        return (ArrayList<LinkedTreeMap>) tmp1.get("result");
    }

    public Double getTotalInOut(ArrayList<LinkedTreeMap> depositsList, ArrayList<LinkedTreeMap> withdrawalsList){
        Double totalBalance = 0.0d;
        for(LinkedTreeMap map : depositsList){
            totalBalance += ((Double) map.get("amount"));
        }
        for(LinkedTreeMap map : withdrawalsList){
            totalBalance -= ((Double) map.get("amount") + (Double) map.get("fee"));
        }
        return totalBalance;
    }



    public void killPosition(Trade trade) throws Exception {
        BigDecimal counterPos = new BigDecimal(trade.openPos, MathContext.DECIMAL64).multiply(new BigDecimal(-1));

        privateApi.privateBuyGet(trade.instrumentName, counterPos, "market", null, null, null, null, null, null, null, null, null);
    }


    public LinkedTreeMap getBookSummary(String instrumentName) throws Exception {
        LinkedTreeMap tmp = (LinkedTreeMap) marketDataApi.publicGetBookSummaryByInstrumentGet(instrumentName);
        ArrayList al = (ArrayList) tmp.get("result");
        return (LinkedTreeMap) al.get(0);
    }



    public static TreeMap<Double, Moment> compileTradeList(ApiController api, boolean includeOpen, ApiController.CURRENCY currency) throws Exception {
        ArrayList<Option> options = Option.parseList(api.getTradeHistory(currency, ApiController.INSTRUMENT.option));
        ArrayList<Delivery> deliveries = Delivery.parseList(api.getSettlementHistory(currency, ApiController.SETTLEMENT.delivery));
        ArrayList<Deposit> deposits = Deposit.parseList(api.getDepositHistory(currency));
        ArrayList<Withdrawal> withdrawals = Withdrawal.parseList(api.getWithdrawalHistory(currency));

        Double index = api.getIndex(currency);
        ArrayList<Trade> trades = Trade.aggregateTrades(options, deliveries, currency, index);
        TreeMap<Double, Movement> sortedMovements = new TreeMap<>();


        for(Deposit dep : deposits){
            //System.out.println("Deposit " + dep.amount + " BTC ID=" + dep.transaction_id);
            sortedMovements.put(dep.getTimestamp(), dep);
        }

        for(Withdrawal wit : withdrawals){
            //System.out.println("Withdrawal " + wit.amount + " BTC ID=" + wit.transaction_id);
            sortedMovements.put(wit.getTimestamp(), wit);
        }

        for(Trade trade : trades){
            //System.out.println(trade.instrumentName + "     Change: " + trade.getChange());
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
