package trading;

import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.*;
import java.math.BigDecimal;

public class BuySell {

    private static Account account;
    private static double moneyPerTrade;

    public static void setAccount(Account account) {
        BuySell.account = account;
    }

    public static Account getAccount() {
        return account;
    }

    //Used by strategy
    public static void open(Currency currency, String explanation, long timestamp) {
        double currentPrice = currency.getPrice(); //Current price of the currency
        double amount = nextAmount() / currency.getPrice();
        if (amount == 0) {
            System.out.println("---OUT OF FUNDS, CANT OPEN TRADE");
            return; //If no fiat is available, we cant trade
        }
        double fiatCost = currentPrice * amount;
        Trade trade = new Trade(currency, currentPrice, amount, explanation);
        currency.setActiveTrade(trade);

        //Converting fiat value to coin value
        account.addToFiat(-fiatCost);
        account.addToWallet(currency, amount);
        account.openTrade(trade);


        String message = "---" + Formatter.formatDate(trade.getOpenTime())
                + " opened trade (" + amount + " "
                + currency.getCoin() + "), at " + currency.getPrice()
                + "\n------" + explanation;
        System.out.println(message);
        if (Mode.get().equals(Mode.BACKTESTING)) currency.appendLogLine(message);
    }

    public static void setMoneyPerTrade(double moneyPerTrade) {
        BuySell.moneyPerTrade = moneyPerTrade;
    }

    //Used by trade
    public static void close(Trade trade) {
        //Converting coin value back to fiat
        trade.setClosePrice(trade.getCurrency().getPrice());
        trade.setCloseTime(trade.getCurrency().getCurrentTime());
        account.closeTrade(trade);
        account.removeFromWallet(trade.getCurrency(), trade.getAmount());
        account.addToFiat(trade.getAmount() * trade.getClosePrice());
        trade.getCurrency().setActiveTrade(null);
        String message = "---" + (Formatter.formatDate(trade.getCloseTime())) + " closed trade ("
                + trade.getAmount() + " " + trade.getCurrency().getCoin()
                + "), at " + trade.getClosePrice()
                + ", with " + Formatter.formatPercent(trade.getProfit()) + " profit";
        System.out.println(message);
        if (Mode.get().equals(Mode.BACKTESTING)) trade.getCurrency().appendLogLine(message);
    }

    private static double nextAmount() {
        return Math.min(account.getFiat(), account.getTotalValue() * moneyPerTrade);
    }

    public static void placeBuyOrder(String currencySymbol, double quantity) throws BinanceApiException {
        BinanceApi api = CurrentAPI.get();
        BinanceSymbol symbol = new BinanceSymbol(currencySymbol);
        BinanceOrderPlacement placement = new BinanceOrderPlacement(symbol, BinanceOrderSide.BUY);
        placement.setType(BinanceOrderType.MARKET);
        placement.setQuantity(BigDecimal.valueOf(quantity));
        BinanceOrder order = api.getOrderById(symbol, api.createOrder(placement).get("orderId").getAsLong());
        System.out.println(order.toString());
    }

    public static void placeSellOrder(String currencySymbol, double quantity) throws BinanceApiException {
        BinanceApi api = CurrentAPI.get();
        BinanceSymbol symbol = new BinanceSymbol(currencySymbol);
        BinanceOrderPlacement placement = new BinanceOrderPlacement(symbol, BinanceOrderSide.SELL);
        placement.setType(BinanceOrderType.MARKET);
        placement.setQuantity(BigDecimal.valueOf(quantity));
        BinanceOrder order = api.getOrderById(symbol, api.createOrder(placement).get("orderId").getAsLong());
        System.out.println(order.toString());
    }
}
