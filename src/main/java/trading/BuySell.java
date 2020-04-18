package trading;

import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.*;
import java.math.BigDecimal;

public class BuySell {

    private static Account account;

    public static void setAccount(Account account) {
        BuySell.account = account;
    }

    public static Account getAccount() {
        return account;
    }

    //Used by strategy
    public static void open(Currency currency, String explanation) {
        double currentPrice = currency.getPrice(); //Current price of the currency
        double amount = nextAmount() / currency.getPrice();
        if (amount == 0) {
            System.out.println("---OUT OF FUNDS, CANT OPEN TRADE");
            return; //If no fiat is available, we cant trade
        }
        double fiatCost = currentPrice * amount;
        Trade trade = new Trade(currency, currentPrice, amount, 0.006, explanation);
        currency.setActiveTrade(trade);

        //Converting fiat value to coin value
        account.addToFiat(-fiatCost);
        account.addToWallet(currency, amount);
        account.openTrade(trade);

        System.out.println("---" + Formatter.formatDate(trade.getEntryTime()) + " opened trade (" + amount + " " + currency.getCoin() + "), at " + currency.getPrice());
        System.out.println("------" + explanation);
    }

    //Used by trade
    public static void close(Trade trade) {
        //Converting coin value back to fiat
        account.closeTrade(trade);
        account.removeFromWallet(trade.getCurrency(), trade.getAmount());
        account.addToFiat(trade.getAmount() * trade.getClosePrice());
        trade.getCurrency().setActiveTrade(null);
        System.out.println("---" + (Formatter.formatDate(trade.getCloseTime())) + " closed trade ("
                + trade.getAmount() + " " + trade.getCurrency().getCoin()
                + "), at " + trade.getClosePrice()
                + ", with " + Formatter.formatPercent(trade.getProfit()) + " profit");
    }

    private static double nextAmount() {
        return Math.min(account.getFiat(), account.getTotalValue() * 0.10);
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
        placement.setPrice(BigDecimal.valueOf(0.00001));
        placement.setQuantity(BigDecimal.valueOf(10000)); // buy 10000 of asset for 0.00001 BTC
        BinanceOrder order = api.getOrderById(symbol, api.createOrder(placement).get("orderId").getAsLong());
        System.out.println(order.toString());
    }
}
