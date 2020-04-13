import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Account {
    private final String username;

    //To give the account a specific final amount of money.
    private double fiatValue;
    private final double startingValue;
    private final ConcurrentHashMap<Currency, Double> wallet;
    private final List<Trade> tradeHistory;
    private final List<Trade> activeTrades;


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
        activeTrades = new ArrayList<>();
    }

    //All Trade methods
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
     * Method has Currency names as keys and the amount of certain currency as value.
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
}
