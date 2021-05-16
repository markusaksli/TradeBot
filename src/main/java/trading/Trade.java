package trading;

import data.config.Config;
import system.Formatter;

public class Trade {

    private double high; //Set the highest price

    private final long openTime;
    private final double entryPrice; //Starting price of a trade (when logic decides to buy)
    private final Currency currency; //What cryptocurrency is used.
    private double amount; //How much are you buying or selling. I.E 6 bitcoins or smth.
    private double closePrice;
    private long closeTime;
    private String explanation;

    public Trade(Currency currency, double entryPrice, double amount, String explanation) {
        this.currency = currency;
        this.entryPrice = entryPrice;
        this.high = entryPrice;
        this.amount = amount;
        this.explanation = explanation;
        openTime = currency.getCurrentTime();
        closePrice = -1;
    }

    //Getters and setters
    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public void setClosePrice(double closePrice) {
        this.closePrice = closePrice;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public double getClosePrice() {
        return closePrice;
    }

    public Currency getCurrency() { //for getting the currency to calculate what the price is now.
        return currency;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public long getOpenTime() {
        return openTime;
    }

    public boolean isClosed() {
        return closePrice != -1;
    }

    //Allows user to get the profit percentages on one specific trade.
    public double getProfit() {
        if (closePrice == -1) {
            return (currency.getPrice() - entryPrice) / entryPrice;
        } else {
            return (closePrice - entryPrice) / entryPrice;
        }
    }

    public long getDuration() {
        return (closeTime - openTime);
    }

    //Checks if there is a new highest price for the trade or if the trade has dropped below the stoploss.
    public void update(double newPrice, int confluence) {
        if (newPrice > high) high = newPrice;

        //TP
        if (getProfit() > Config.get(this).getTakeProfit()) {
            explanation += "Closed due to: Take profit";
            currency.getAccount().close(this);
            return;
        }

        //Trailing SL
        if (newPrice < high * (1 - Config.get(this).getTrailingSl())) {
            explanation += "Closed due to: Trailing SL";
            currency.getAccount().close(this);
            return;
        }

        //Confluence to close
        if (Config.get(this).useConfluenceToClose() && confluence <= -Config.get(this).getConfluenceToClose()) {
            explanation += "Closed due to: Indicator confluence of " + confluence;
            currency.getAccount().close(this);
        }
    }

    @Override
    public String toString() {
        return (isClosed() ? (currency.getAccount().getTradeHistory().indexOf(this) + 1) : (currency.getAccount().getActiveTrades().indexOf(this) + 1)) + " "
                + currency.getPair() + " " + Formatter.formatDecimal(amount) + "\n"
                + "open: " + Formatter.formatDate(openTime) + " at " + entryPrice + "\n"
                + (isClosed() ? "close: " + Formatter.formatDate(closeTime) + " at " + closePrice : "current price: " + currency.getPrice()) + "\n"
                + "high: " + high + ", profit: " + Formatter.formatPercent(getProfit()) + "\n"
                + explanation + "\n";
    }
}
