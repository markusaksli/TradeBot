import java.sql.Timestamp;
import java.util.List;

public class BuySell {
    public void buy(Currency currency, Account account, double amount) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis()); //Creating timestamp to have the time of trade
        double currentPrice = currency.getPrice(); //Current price of the currenct
        double priceUSD = currentPrice * amount;
        account.subtractDollars(priceUSD);
        //TODO: This trade should have more specifications in the future, right now lets settle with this.
        Trade trade = new Trade(currentPrice, timestamp, amount);
        //Adding all necessary to wallet
        account.addToWallet(currency, amount);
        account.addTrade(trade);

    }
    public void sell(Currency currency, Account account, double amount) {
        List<Trade> accountTrades = account.getCurrentTrades();
        double profitMoney = 0;
        double closePrice = currency.getPrice();
        for (Trade trade : accountTrades) {
            //If trade matches the specifications of a
            if (trade.getCurrency().getName().equals(currency.getName()) && trade.getAmountOfCurrency() == amount) {
                trade.setClosePrice(closePrice);
                account.addTradeHistory(trade);
                account.removeTrade(trade);
                double profitUSD = trade.getProfitUSD();
                double startingPrice = trade.getEntryPrice();
                profitMoney = Math.abs(profitUSD * startingPrice); //Using abs, because method subtract dollars subtracts,
                //so if we have a negative number as profitMoney because we lost money, it would add. Now it wont.
                break;
            }
        }
        account.subtractDollars(profitMoney);
        account.removeFromWallet(currency, amount);
    }
}
