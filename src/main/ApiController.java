package main;

import com.google.gson.internal.LinkedTreeMap;
import movements.Trade;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.api.AccountManagementApi;
import org.openapitools.client.api.MarketDataApi;
import org.openapitools.client.api.TradingApi;
import org.openapitools.client.api.WalletApi;
import org.openapitools.client.auth.HttpBasicAuth;

import java.math.BigDecimal;
import java.util.ArrayList;

public class ApiController {

    private ApiClient defaultClient;

    private TradingApi tradingApi;
    private AccountManagementApi accountManagementApi;
    private WalletApi walletApi;
    private MarketDataApi marketDataApi;

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
        BigDecimal counterPos = new BigDecimal(trade.openPos * -1.0);
        tradingApi.privateBuyGet(trade.instrumentName, counterPos, "market", "Sentry", null, "immediate_or_cancel", null, null, null, null, null, null);
    }


    public LinkedTreeMap getBookSummary(String instrumentName) throws Exception {
        LinkedTreeMap tmp = (LinkedTreeMap) marketDataApi.publicGetBookSummaryByInstrumentGet(instrumentName);
        ArrayList al = (ArrayList) tmp.get("result");
        return (LinkedTreeMap) al.get(0);
    }
}
