package trading;


import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.exception.BinanceApiException;
import system.Formatter;
import system.Mode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

public class BuySell {

    private static LocalAccount localAccount;
    private static double moneyPerTrade;

    public static void setAccount(LocalAccount localAccount) {
        BuySell.localAccount = localAccount;
    }

    public static LocalAccount getAccount() {
        return localAccount;
    }

    //Used by strategy
    public static void open(Currency currency, String explanation, long timestamp) {
        double currentPrice = currency.getPrice(); //Current price of the currency
        double amount = nextAmount() / currency.getPrice();
        
        // round amount
        int precision = CurrentAPI.get().getExchangeInfo().getSymbolInfo(currency.getPair()).getBaseAssetPrecision();
        BigDecimal bigDecimal = new BigDecimal(Double.toString(amount));
        bigDecimal = bigDecimal.setScale(precision, RoundingMode.HALF_DOWN);
        amount = bigDecimal.doubleValue();

        if (amount == 0) {
            System.out.println("---OUT OF FUNDS, CANT OPEN TRADE");
            return; //If no fiat is available, we cant trade
        }
        // check LOT_SIZE
        int minQty = CurrentAPI.get().getExchangeInfo().getSymbolInfo(currency.getPair()).getFilters().stream().filter(f -> FilterType.LOT_SIZE == f.getFilterType()).findFirst().map( f1 -> f1.getMinQty()).map(Integer::parseInt).get();
        if (amount < minQty) {
            System.out.println("---OUT OF LOT_SIZE, CANT OPEN TRADE. min LOT_SIZE=" + minQty);
            return;
        }
        double fiatCost = currentPrice * amount;
        Trade trade = new Trade(currency, currentPrice, amount, explanation);
        currency.setActiveTrade(trade);

        //Converting fiat value to coin value
        localAccount.addToFiat(-fiatCost);
        localAccount.addToWallet(currency, amount);
        localAccount.openTrade(trade);
        if (Mode.get().equals(Mode.LIVE)) {
            try {
                placeOrder(currency.getPair(), amount, true);
            } catch (BinanceApiException e) {
                e.printStackTrace();
            }
        }

        String message = "---" + Formatter.formatDate(trade.getOpenTime())
                + " opened trade (" + Formatter.formatDecimal(amount) + " "
                + currency.getPair() + "), at " + currency.getPrice();
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
        localAccount.closeTrade(trade);
        localAccount.removeFromWallet(trade.getCurrency(), trade.getAmount());
        localAccount.addToFiat(trade.getAmount() * trade.getClosePrice());
        trade.getCurrency().setActiveTrade(null);

        if (Mode.get().equals(Mode.LIVE)) {
            try {
                placeOrder(trade.getCurrency().getPair(), trade.getAmount(), false);
            } catch (BinanceApiException e) {
                e.printStackTrace();
            }
        }

        String message = "---" + (Formatter.formatDate(trade.getCloseTime())) + " closed trade ("
                + Formatter.formatDecimal(trade.getAmount()) + " " + trade.getCurrency().getPair()
                + "), at " + trade.getClosePrice()
                + ", with " + Formatter.formatPercent(trade.getProfit()) + " profit"
                + "\n------" + trade.getExplanation();
        System.out.println(message);
        if (Mode.get().equals(Mode.BACKTESTING)) trade.getCurrency().appendLogLine(message);
    }

    private static double nextAmount() {
        if (Mode.get().equals(Mode.BACKTESTING)) return localAccount.getFiat();
        return Math.min(localAccount.getFiat(), localAccount.getTotalValue() * moneyPerTrade);
    }

    //TODO: Check buy/sell with live account
    //TODO: Implement limit ordering
    public static void placeOrder(String currencySymbol, double quantity, boolean buy) throws BinanceApiException {
        BinanceApiRestClient client = CurrentAPI.get();
        NewOrderResponse newOrderResponse = client.newOrder(
                buy ?
                        marketBuy(currencySymbol, String.valueOf(quantity)).newOrderRespType(NewOrderResponseType.FULL) :
                        marketSell(currencySymbol, String.valueOf(quantity)).newOrderRespType(NewOrderResponseType.FULL));
        List<com.binance.api.client.domain.account.Trade> fills = newOrderResponse.getFills();
        System.out.println("Placed" + (buy ? "buy" : "sell") + " order with id " + newOrderResponse.getClientOrderId());
    }
}
