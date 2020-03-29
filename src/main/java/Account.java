public class Account {
    private String username;

    //To give the account a specific final amount of money.
    private final double dollars;

    //TODO uncomment the 2 lines below when Trades class is ready.
    //private List<Trades> tradeHistory;
    //private List<Trades> currentTrades;
    private double wallet;

    /**
     * Wallet value will most probably be 0 at first, but you could start
     * with an existing wallet value as well.
     */
    public Account(String username, double dollars, double wallet) {
        this.username = username;
        this.dollars = dollars;
        this.wallet = wallet;
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
        //TODO replace "return profit" with following code when Trades is ready
        /*
        for (Trade trade : currentTrades) {
            profit += trade.getProfit();
        }
        return profit
         */
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
