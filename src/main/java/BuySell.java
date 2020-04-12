import java.sql.Timestamp;

public class BuySell {

    private static Account account;

    public static void setAccount(Account account) {
        BuySell.account = account;
    }

    public static Account getAccount() {
        return account;
    }

    //Used by strategy
    public static void open(Currency currency, double amount) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis()); //Creating timestamp to have the time of trade
        double currentPrice = currency.getPrice(); //Current price of the currenct
        double fiatCost = -currentPrice * amount;
        account.addToFiat(fiatCost);
        //TODO: This trade should have more specifications in the future, right now lets settle with this.
        Trade trade = new Trade(currentPrice, timestamp, amount);
        //Adding all necessary to wallet
        account.addToWallet(currency, amount);
        account.openTrade(trade);

    }

    //Used by trade
    public static void close(Trade trade) {
        account.closeTrade(trade);
        account.removeFromWallet(trade.getCurrency(), trade.getAmount());
    }
}
