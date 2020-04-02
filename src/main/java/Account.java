import java.util.HashMap;
import java.util.List;

public class Account {
    private String username;

    //To give the account a specific final amount of money.
    private double dollars;
    private HashMap<String, Double> wallet;
    private List<Trade> tradeHistory;
    private List<Trade> currentTrades;


    /**
     * Wallet value will most probably be 0 at first, but you could start
     * with an existing wallet value as well.
     */
    public Account(String username, double dollars) {
        this.username = username;
        this.dollars = dollars;
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
    }

    public void addTradeHistory(Trade trade) {
        tradeHistory.add(trade);
    }


    //All the get methods.
    public String getUsername() {
        return username;
    }
    public double getDollars() {
        return dollars;
    }
    public void subtractDollars(double amount) {
        dollars = dollars - amount;
    }

    /**
     * Method has Currency names as keys and the amount of certain currency as value.
     * i.e {"BTCUSDT : 3.23}
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
        for (Trade trade : currentTrades) {
            double startingPrice = trade.getFillPrice();
            Currency tradeCurrency = trade.getCurrency();
            double percentages = Math.round((tradeCurrency.getPrice() * 100 / startingPrice - 100) * 1000);
            percentages = percentages / 1000; //Calculate how many percentages has risen or fallen since start.
            //If negative then profit is negative, otherwise positive.
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
