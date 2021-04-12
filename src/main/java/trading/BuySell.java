package trading;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.api.client.exception.BinanceApiException;
import system.ConfigSetup;
import system.Formatter;
import system.Mode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

public class BuySell {

    private static LocalAccount localAccount;
    public static double MONEY_PER_TRADE;

    public static void setAccount(LocalAccount localAccount) {
        BuySell.localAccount = localAccount;
    }

    public static LocalAccount getAccount() {
        return localAccount;
    }

    private BuySell() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean enoughFunds() {
        return nextAmount() != 0;
    }

    //Used by strategy
    public static void open(Currency currency, String explanation) {
        if (currency.hasActiveTrade()) {
            System.out.println("---Cannot open trade since there already is an open trade for " + currency.getPair() + "!");
            return;
        }
        if (!enoughFunds()) {
            System.out.println("---Out of funds, cannot open trade! (" + Formatter.formatDecimal(localAccount.getFiat()) + ")");
            return; //If no fiat is available, we cant trade
        }

        double currentPrice = currency.getPrice(); //Current price of the currency
        double fiatCost = nextAmount();
        double amount = fiatCost / currency.getPrice();

        Trade trade;
        if (Mode.get().equals(Mode.LIVE)) {
            NewOrderResponse order = placeOrder(currency, amount, true);
            if (order == null) {
                return;
            }
            double fillsQty = 0;
            double fillsPrice = 0;

            for (com.binance.api.client.domain.account.Trade fill : order.getFills()) {
                double qty = Double.parseDouble(fill.getQty());
                fillsQty += qty - Double.parseDouble(fill.getCommission());
                fillsPrice += qty * Double.parseDouble(fill.getPrice());
            }
            System.out.println("Got filled for " + BigDecimal.valueOf(fillsQty).toString()
                    + " at " + Formatter.formatDate(order.getTransactTime())
                    + ", at a price of " + Formatter.formatDecimal(fillsPrice) + " " + ConfigSetup.getFiat());
            fiatCost = fillsPrice;
            amount = fillsQty;
            trade = new Trade(currency, fillsPrice / fillsQty, amount, explanation);
            System.out.println("Opened trade at an avg open of " + Formatter.formatDecimal(trade.getEntryPrice()) + " ("
                    + Formatter.formatPercent((trade.getEntryPrice() - currentPrice) / trade.getEntryPrice())
                    + " from current)");
        } else {
            trade = new Trade(currency, currentPrice, amount, explanation);
        }

        currency.setActiveTrade(trade);

        //Converting fiat value to coin value
        localAccount.addToFiat(-fiatCost);
        localAccount.addToWallet(currency, amount);
        localAccount.openTrade(trade);

        String message = "---" + Formatter.formatDate(trade.getOpenTime())
                + " opened trade (" + Formatter.formatDecimal(trade.getAmount()) + " "
                + currency.getPair() + "), at " + Formatter.formatDecimal(trade.getEntryPrice())
                + ", " + trade.getExplanation();
        System.out.println(message);
        if (Mode.get().equals(Mode.BACKTESTING)) currency.appendLogLine(message);
    }

    //Used by trade
    public static void close(Trade trade) {
        if (Mode.get().equals(Mode.LIVE)) {
            NewOrderResponse order = placeOrder(trade.getCurrency(), trade.getAmount(), false);
            if (order == null) {
                return;
            }
            double fillsQty = 0;
            double fillsPrice = 0;
            for (com.binance.api.client.domain.account.Trade fill : order.getFills()) {
                double qty = Double.parseDouble(fill.getQty());
                fillsQty += qty;
                fillsPrice += qty * Double.parseDouble(fill.getPrice()) - Double.parseDouble(fill.getCommission());
            }
            System.out.println("Got filled for " + BigDecimal.valueOf(fillsQty).toString()
                    + " at " + Formatter.formatDate(order.getTransactTime())
                    + ", at a price of " + Formatter.formatDecimal(fillsPrice) + " " + ConfigSetup.getFiat());
            trade.setClosePrice(fillsPrice / fillsQty);
            trade.setCloseTime(order.getTransactTime());
            localAccount.removeFromWallet(trade.getCurrency(), fillsQty);
            localAccount.addToFiat(fillsPrice);
            System.out.println("Closed trade at an avg close of " + Formatter.formatDecimal(trade.getClosePrice()) + " ("
                    + Formatter.formatPercent((trade.getClosePrice() - trade.getCurrency().getPrice()) / trade.getClosePrice())
                    + " from current)");
        } else {
            trade.setClosePrice(trade.getCurrency().getPrice());
            trade.setCloseTime(trade.getCurrency().getCurrentTime());
            localAccount.removeFromWallet(trade.getCurrency(), trade.getAmount());
            localAccount.addToFiat(trade.getAmount() * trade.getClosePrice());
        }

        //Converting coin value back to fiat
        localAccount.closeTrade(trade);
        trade.getCurrency().setActiveTrade(null);

        String message = "---" + (Formatter.formatDate(trade.getCloseTime())) + " closed trade ("
                + Formatter.formatDecimal(trade.getAmount()) + " " + trade.getCurrency().getPair()
                + "), at " + Formatter.formatDecimal(trade.getClosePrice())
                + ", with " + Formatter.formatPercent(trade.getProfit()) + " profit"
                + "\n------" + trade.getExplanation();
        System.out.println(message);
        if (Mode.get().equals(Mode.BACKTESTING)) trade.getCurrency().appendLogLine(message);
    }

    private static double nextAmount() {
        if (Mode.get().equals(Mode.BACKTESTING)) return localAccount.getFiat();
        return Math.min(localAccount.getFiat(), localAccount.getTotalValue() * MONEY_PER_TRADE);
    }


    //TODO: Implement limit ordering
    public static NewOrderResponse placeOrder(Currency currency, double amount, boolean buy) {
        System.out.println("\n---Placing a " + (buy ? "buy" : "sell") + " market order for " + currency.getPair());
        BigDecimal originalDecimal = BigDecimal.valueOf(amount);
        //Round amount to base precision and LOT_SIZE
        int precision = CurrentAPI.get().getExchangeInfo().getSymbolInfo(currency.getPair()).getBaseAssetPrecision();
        String lotSize;
        Optional<String> minQtyOptional = CurrentAPI.get().getExchangeInfo().getSymbolInfo(currency.getPair()).getFilters().stream().filter(f -> FilterType.LOT_SIZE == f.getFilterType()).findFirst().map(f1 -> f1.getMinQty());
        Optional<String> minNotational = CurrentAPI.get().getExchangeInfo().getSymbolInfo(currency.getPair()).getFilters().stream().filter(f -> FilterType.MIN_NOTIONAL == f.getFilterType()).findFirst().map(SymbolFilter::getMinNotional);
        if (minQtyOptional.isPresent()) {
            lotSize = minQtyOptional.get();
        } else {
            System.out.println("---Could not get LOT_SIZE so could not open trade!");
            return null;
        }
        double minQtyDouble = Double.parseDouble(lotSize);

        //Check LOT_SIZE to make sure amount is not too small
        if (amount < minQtyDouble) {
            System.out.println("---Amount smaller than min LOT_SIZE, could not open trade! (min LOT_SIZE=" + lotSize + ", amount=" + amount);
            return null;
        }

        //Convert amount to an integer multiple of LOT_SIZE and convert to asset precision
        System.out.println("Converting from double trade amount " + originalDecimal.toString() + " to base asset precision " + precision + " LOT_SIZE " + lotSize);
        String convertedAmount = new BigDecimal(lotSize).multiply(new BigDecimal((int) (amount / minQtyDouble))).setScale(precision, RoundingMode.HALF_DOWN).toString();
        System.out.println("Converted to " + convertedAmount);

        if (minNotational.isPresent()) {
            double notational = Double.parseDouble(convertedAmount) * currency.getPrice();
            if (notational < Double.parseDouble(minNotational.get())) {
                System.out.println("---Cannot open trade because notational value " + Formatter.formatDecimal(notational) + " is smaller than minimum " + minNotational.get());
            }
        }

        NewOrderResponse order;
        try {
            BinanceApiRestClient client = CurrentAPI.get();
            order = client.newOrder(
                    buy ?
                            marketBuy(currency.getPair(), convertedAmount).newOrderRespType(NewOrderResponseType.FULL) :
                            marketSell(currency.getPair(), convertedAmount).newOrderRespType(NewOrderResponseType.FULL));
            System.out.println("---Executed a " + order.getSide() + " order with id " + order.getClientOrderId() + " for " + convertedAmount + " " + currency.getPair());
            if (!order.getStatus().equals(OrderStatus.FILLED)) {
                System.out.println("Order is " + order.getStatus() + ", not FILLED!");
            }
            return order;
        } catch (BinanceApiException e) {
            System.out.println("---Failed " + (buy ? "buy" : "sell") + " " + convertedAmount + " " + currency.getPair());
            System.out.println(e.getMessage());
            return null;
        }
    }
}
