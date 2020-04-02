import java.sql.Timestamp;
import java.util.List;

public class BuySell {
    public void buy(Currency currency, Account account, double amount) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis()); //Creating timestamp to have the time of trade
        double currentPrice = currency.getPrice(); //Current price of the currenct
        //TODO: This trade should have more specifications in the future, right now lets settle with this.
        Trade trade = new Trade(currentPrice, timestamp, amount);
        //Adding all necessary to wallet
        account.addToWallet(currency.getName(), amount);
        account.addTrade(trade);

    }
    public void sell(Currency currency, Account account, double amount) {
        List<Trade> accountTrades = account.getCurrentTrades();
        for (Trade trade : accountTrades) {
            //If trade matches the specifications of a
            if (trade.getCurrency().getName().equals(currency.getName()) && trade.getAmountOfCurrency() == amount) {
                account.addTradeHistory(trade);
                account.removeTrade(trade);
            }
        }
        account.removeFromWallet(currency.getName(), amount);
    }
}
