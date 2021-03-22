package trading;

import com.binance.api.client.domain.account.Account;
import com.binance.api.client.exception.BinanceApiException;
import system.ConfigSetup;
import system.Formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LocalAccount {
    private final String username;
    private Account realAccount;

    //To give the account a specific final amount of money.
    private double fiatValue;
    private double startingValue;
    private final ConcurrentHashMap<Currency, Double> wallet;
    private final List<Trade> tradeHistory;
    private final List<Trade> activeTrades;
    private double makerComission;
    private double takerComission;
    private double buyerComission;

    /**
     * Wallet value will most probably be 0 at first, but you could start
     * with an existing wallet value as well.
     */
    public LocalAccount(String username, double startingValue) {
        this.username = username;
        this.startingValue = startingValue;
        fiatValue = startingValue;
        wallet = new ConcurrentHashMap<>();
        tradeHistory = new ArrayList<>();
        activeTrades = new CopyOnWriteArrayList<>();
    }

    public LocalAccount(String apiKey, String secretApiKey) {
        CurrentAPI.login(apiKey, secretApiKey);
        username = "";
        wallet = new ConcurrentHashMap<>();
        tradeHistory = new ArrayList<>();
        activeTrades = new CopyOnWriteArrayList<>();
        realAccount = CurrentAPI.get().getAccount();
        if (!realAccount.isCanTrade()) {
            System.out.println("Can't trade!");
        }
        makerComission = realAccount.getMakerCommission(); //Maker fees are
        // paid when you add liquidity to our order book
        // by placing a limit order below the ticker price for buy, and above the ticker price for sell.
        takerComission = realAccount.getTakerCommission();//Taker fees are paid when you remove
        // liquidity from our order book by placing any order that is executed against an order on the order book.
        buyerComission = realAccount.getBuyerCommission();

        //Example: If the current market/ticker price is $2000 for 1 BTC and you market buy bitcoins starting at the market price of $2000, then you will pay the taker fee. In this instance, you have taken liquidity/coins from the order book.
        //
        //If the current market/ticker price is $2000 for 1 BTC and you
        //place a limit buy for bitcoins at $1995, then
        //you will pay the maker fee IF the market/ticker price moves into your limit order at $1995.
        fiatValue = Double.parseDouble(realAccount.getAssetBalance(ConfigSetup.getFiat()).getFree());
        System.out.println("---Starting FIAT: " + Formatter.formatDecimal(fiatValue) + " " + ConfigSetup.getFiat());
    }

    public Account getRealAccount() {
        return realAccount;
    }

    //All backend.Trade methods
    public List<Trade> getActiveTrades() {
        return activeTrades;
    }

    public List<Trade> getTradeHistory() {
        return tradeHistory;
    }

    public void setStartingValue(double startingValue) {
        this.startingValue = startingValue;
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

    public void setFiat(double fiatValue) {
        this.fiatValue = fiatValue;
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
