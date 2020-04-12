import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BuySell {

    private static Account account;
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

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
        double fiatCost = currentPrice * amount;
        account.addToFiat(-fiatCost);
        //TODO: This trade should have more specifications in the future, right now lets settle with this.
        Trade trade = new Trade(currency, currentPrice, timestamp, amount, 0.015);
        //Adding all necessary to wallet
        account.addToWallet(currency, amount);
        account.openTrade(trade);
        currency.setActiveTrade(trade);

        System.out.println("Opened trade (" + currency.getSymbol() + ", " + fiatCost + ")" + " at " + dtf.format(LocalDateTime.now()));
    }

    //Used by trade
    public static void close(Trade trade) {
        account.closeTrade(trade);
        account.removeFromWallet(trade.getCurrency(), trade.getAmount());
        trade.getCurrency().setActiveTrade(null);
        System.out.println("Closed trade (" + trade.getCurrency().getSymbol() + ", profit " + trade.getProfit() + ")" + " at " + dtf.format(LocalDateTime.now()));
        System.out.println("Account profit: " + account.getProfit() * 100 + "%");
    }
}
