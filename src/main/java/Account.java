import java.util.List;

public class Account {
    private String username;

    //To give the account a specific final amount of money.
    private final double dollars;
    private List<Currency> currencies;
    private List<Trade> tradeHistory;
    private List<Trade> currentTrades;
    private double wallet;

    /**
     * Wallet value will most probably be 0 at first, but you could start
     * with an existing wallet value as well.
     */
    public Account(String username, double dollars, double wallet, List<Currency> currencies) {
        this.username = username;
        this.dollars = dollars;
        this.wallet = wallet;
        this.currencies = currencies;
    }

    //All the get methods.
    public String getUsername() {
        return username;
    }

    public double getDollars() {
        return dollars;
    }

    public double getWallet() {
        return wallet;
    }

    /**
     * Method will calculate current profit off of all the active trades
     *
     * @return returns the sum of all the profits
     */
    public double getProfit() {
        double profit = 0;
        for (Trade trade : currentTrades) {
            double startingPrice = trade.getFillPrice();
            Currency tradeCurrency = trade.getCurrency();
            double percentages = (tradeCurrency.getPrice() * startingPrice) / 100; //Calculate how many percentages has risen or fallen since start.

            if (startingPrice * percentages < startingPrice) { //If has fallen, then subtract.
                profit -= startingPrice - startingPrice * percentages;
            } else if (startingPrice * percentages > startingPrice) { //If has risen then add.
                profit += startingPrice + startingPrice * percentages;
            } else { //If nothing has changed.
                profit += 0;
            }
        }
        return profit;
    }

    //Allows you to add to the wallet easily.
    public void addToWallet(double amount) {
        wallet += amount;
    }

    /**
     * Method calculates the sum of the entire portfolio.
     * That includes free money to invest + everything currently in trades.
     */
    public double getEntirePortfolio() {
        return wallet + getProfit();
    }


}
