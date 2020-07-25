package trading;


import com.binance.api.client.exception.BinanceApiException;

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
        if (amount == 0) {
            System.out.println("---OUT OF FUNDS, CANT OPEN TRADE");
            return; //If no fiat is available, we cant trade
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
                placeBuyOrder(currency.getPair(), amount);
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
                placeSellOrder(trade.getCurrency().getPair(), trade.getAmount());
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

    //TODO: Fix buy and sell methods for live trading
    public static void placeBuyOrder(String currencySymbol, double quantity) throws BinanceApiException {
        /*BinanceApi api = CurrentAPI.get();
        BinanceSymbol symbol = new BinanceSymbol(currencySymbol);
        BinanceOrderPlacement placement = new BinanceOrderPlacement(symbol, BinanceOrderSide.BUY);
        placement.setType(BinanceOrderType.MARKET);
        placement.setQuantity(BigDecimal.valueOf(quantity));
        BinanceOrder order = api.getOrderById(symbol, api.createOrder(placement).get("orderId").getAsLong());
        System.out.println(order.toString());*/
    }

    public static void placeSellOrder(String currencySymbol, double quantity) throws BinanceApiException {
        /*BinanceApi api = CurrentAPI.get();
        BinanceSymbol symbol = new BinanceSymbol(currencySymbol);
        BinanceOrderPlacement placement = new BinanceOrderPlacement(symbol, BinanceOrderSide.SELL);
        placement.setType(BinanceOrderType.MARKET);
        placement.setQuantity(BigDecimal.valueOf(quantity));
        BinanceOrder order = api.getOrderById(symbol, api.createOrder(placement).get("orderId").getAsLong());
        System.out.println(order.toString());*/
    }
}
