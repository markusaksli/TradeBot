import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Account {
    private final String username;

    //To give the account a specific final amount of money.
    private double value;
    //TODO: Change to currency, BigDecimal?
    private final HashMap<String, Double> wallet;
    private final List<Trade> tradeHistory;
    private final List<Trade> currentTrades;


    /**
     * Wallet value will most probably be 0 at first, but you could start
     * with an existing wallet value as well.
     */
    public Account(String username, double value) {
        this.username = username;
        this.value = value;
        wallet = new HashMap<>();
        tradeHistory = new ArrayList<>();
        currentTrades = new ArrayList<>();
    }

    //All Trade methods
    public List<Trade> getCurrentTrades() {
        return currentTrades;
    }

    public void addTrade(Trade trade) {
        currentTrades.add(trade);
    }

    public void removeTrade(Trade trade) {
        currentTrades.remove(trade);
        tradeHistory.add(trade);
    }

    public void addTradeHistory(Trade trade) {
        tradeHistory.add(trade);
    }


    //All the get methods.
    public String getUsername() {
        return username;
    }

    public double getValue() {
        return value;
    }

    public void subtractDollars(double amount) {
        value = value - amount;
    }

    /**
     * Method has Currency names as keys and the amount of certain currency as value.
     * i.e {"BTCUSDT : 3.23}
     *
     * @return
     */
    public HashMap<String, Double> getWallet() {
        return wallet;
    }

    /**
     * Method will calculate current profit off of all the active trades
     *
     * @return returns the sum of all the percentages wether the profit is below 0 or above.
     */
    public double getWholeProfit() {
        double profit = 0;
        double percentages = 0;
        for (Trade trade : currentTrades) {
            percentages = trade.getProfitUSD();
            if (percentages < 0) {
                profit -= percentages;
            } else if (percentages >= 0) {
                profit += percentages;
            }
        }
        return profit; //return total percentages
    }


    //All wallet methods

    /**
     * Method allows to add currencies to wallet hashmap.
     *
     * @param key Should be the name of the currency ie "BTCUSDT"
     * @param value The amound how much was bought.
     */
    public void addToWallet(String key, double value) {
        if (wallet.containsKey(key)) {
            double previousValue = wallet.get(key);
            wallet.put(key, value + previousValue);
        } else {
            wallet.put(key,value);
        }
    }

    /**
     * Method allows to remove values from keys.
     **/
    public void removeFromWallet(String key, double value) {
        if (wallet.containsKey(key)) {
            double currentValue = wallet.get(key);
            if (currentValue >= value) {
                wallet.put(key, currentValue - value);
            }
        }
    }


}
