package trading;

import com.google.gson.JsonObject;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceOrder;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Account {
    private final String username;

    //To give the account a specific final amount of money.
    private double fiatValue;
    private double startingValue;
    private final ConcurrentHashMap<Currency, Double> wallet;
    private final List<Trade> tradeHistory;
    private final List<Trade> activeTrades;
    private JsonObject account;
    private double makerComission;
    private double takerComission;
    private double buyerComission;

    /**
     * Wallet value will most probably be 0 at first, but you could start
     * with an existing wallet value as well.
     */
    public Account(String username, double startingValue) {
        this.username = username;
        this.startingValue = startingValue;
        fiatValue = startingValue;
        wallet = new ConcurrentHashMap<>();
        tradeHistory = new ArrayList<>();
        activeTrades = new CopyOnWriteArrayList<>();
    }

    public Account(String apiKey, String secretApiKey) {
        CurrentAPI.get().setApiKey(apiKey);
        CurrentAPI.get().setSecretKey(secretApiKey);
        this.startingValue = 0;
        username = "";
        wallet = new ConcurrentHashMap<>();
        tradeHistory = new ArrayList<>();
        activeTrades = new CopyOnWriteArrayList<>();
        initLive();
    }

    //All backend.Trade methods
    public List<Trade> getActiveTrades() {
        return activeTrades;
    }

    public List<Trade> getTradeHistory() {
        return tradeHistory;
    }

    public void openTrade(Trade trade) {
        activeTrades.add(trade);
    }

    public void closeTrade(Trade trade) {
        activeTrades.remove(trade);
        tradeHistory.add(trade);
    }


    //All the get methods.
    public String getUsername() {
        return username;
    }

    public double getFiat() {
        return fiatValue;
    }

    public double getTotalValue() {
        double value = 0;
        for (Map.Entry<Currency, Double> entry : wallet.entrySet()) {
            Currency currency = entry.getKey();
            Double amount = entry.getValue();
            value += amount * currency.getPrice();
        }
        return value + fiatValue;
    }

    public void addToFiat(double amount) {
        fiatValue += amount;
    }

    /**
     * Method has backend.Currency names as keys and the amount of certain currency as value.
     * i.e {"BTCUSDT : 3.23}
     *
     * @return
     */
    public ConcurrentHashMap<Currency, Double> getWallet() {
        return wallet;
    }

    /**
     * Method will calculate current profit off of all the active trades
     *
     * @return returns the sum of all the percentages wether the profit is below 0 or above.
     */
    public double getProfit() {
        return (getTotalValue() - startingValue) / startingValue;
    }



    //All wallet methods
    /**
     * Method allows to add currencies to wallet hashmap.
     *
     * @param key   Should be the name of the currency ie "BTCUSDT"
     * @param value The amount how much was bought.
     */
    public void addToWallet(Currency key, double value) {
        if (wallet.containsKey(key)) {
            wallet.put(key, wallet.get(key) + value);
        } else {
            wallet.put(key, value);
        }

    }

    /**
     * Method allows to remove values from keys.
     **/
    public void removeFromWallet(Currency key, double value) {
        wallet.put(key, wallet.get(key) - value);
    }

    /**
     * Everything connected to live trading in account.
     */
    private void initLive() {
        List<String> currencies = new ArrayList<>();
        try {
            JsonObject account = CurrentAPI.get().account();
            this.account = account;
            makerComission = account.get("makerCommission").getAsBigDecimal().doubleValue(); //Maker fees are
            // paid when you add liquidity to our order book
            // by placing a limit order below the ticker price for buy, and above the ticker price for sell.
            takerComission = account.get("takerCommission").getAsBigDecimal().doubleValue();//Taker fees are paid when you remove
            // liquidity from our order book by placing any order that is executed against an order on the order book.
            buyerComission = account.get("buyerCommission").getAsBigDecimal().doubleValue();

            //Example: If the current market/ticker price is $2000 for 1 BTC and you market buy bitcoins starting at the market price of $2000, then you will pay the taker fee. In this instance, you have taken liquidity/coins from the order book.
            //
            //If the current market/ticker price is $2000 for 1 BTC and you
            //place a limit buy for bitcoins at $1995, then
            //you will pay the maker fee IF the market/ticker price moves into your limit order at $1995.

            if (CurrentAPI.get().balancesMap().isEmpty()) {
                startingValue = 0;
            } else {
                startingValue = CurrentAPI.get().balancesMap().get("USDT").free.doubleValue();
            }
            currencies.addAll(CurrentAPI.get().balancesMap().keySet());
            for (String current : currencies) {
                BinanceSymbol symbol = new BinanceSymbol(current + "USDT");
                Currency currency = new Currency(current);
                addToWallet(currency, 0);
                for (BinanceOrder order : CurrentAPI.get().openOrders(symbol)) {
                    activeTrades.add(new Trade(new Currency(current),
                            order.getPrice().doubleValue(), order.getExecutedQty().doubleValue(),
                            "Already opened before BOT initiated."));
                    addToWallet(currency, order.getExecutedQty().doubleValue());
                }
            }
        } catch (BinanceApiException e) {
            e.printStackTrace();
        }
    }

    public double getMakerComission() {
        return makerComission;
    }

    public double getTakerComission() {
        return takerComission;
    }

    public double getBuyerComission() {
        return buyerComission;
    }
}
