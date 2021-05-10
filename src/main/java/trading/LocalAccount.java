package trading;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.api.client.exception.BinanceApiException;
import system.BinanceAPI;
import system.Formatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

public class LocalAccount {
    private final Instance instance;
    private Account realAccount;
    private BinanceApiRestClient client;

    //To give the account a specific final amount of money.
    private double fiatValue;
    private double startingValue;
    private final ConcurrentHashMap<Currency, Double> wallet;
    private final List<Trade> tradeHistory;
    private final List<Trade> activeTrades;
    private double makerCommission;
    private double takerCommission;
    private double buyerCommission;

    public LocalAccount(Instance instance, double startingValue) {
        this.instance = instance;
        this.startingValue = startingValue;
        fiatValue = startingValue;
        wallet = new ConcurrentHashMap<>();
        tradeHistory = new ArrayList<>();
        activeTrades = new CopyOnWriteArrayList<>();
    }

    public LocalAccount(Instance instance, String apiKey, String secretApiKey) {
        this.instance = instance;
        client = BinanceAPI.login(apiKey, secretApiKey).newRestClient();

        wallet = new ConcurrentHashMap<>();
        tradeHistory = new ArrayList<>();
        activeTrades = new CopyOnWriteArrayList<>();
        realAccount = client.getAccount();
        if (!realAccount.isCanTrade()) {
            System.out.println("Can't trade!");
        }
        makerCommission = realAccount.getMakerCommission(); //Maker fees are
        // paid when you add liquidity to our order book
        // by placing a limit order below the ticker price for buy, and above the ticker price for sell.
        takerCommission = realAccount.getTakerCommission();//Taker fees are paid when you remove
        // liquidity from our order book by placing any order that is executed against an order on the order book.
        buyerCommission = realAccount.getBuyerCommission();

        //Example: If the current market/ticker price is $2000 for 1 BTC and you market buy bitcoins starting at the market price of $2000, then you will pay the taker fee. In this instance, you have taken liquidity/coins from the order book.
        //
        //If the current market/ticker price is $2000 for 1 BTC and you
        //place a limit buy for bitcoins at $1995, then
        //you will pay the maker fee IF the market/ticker price moves into your limit order at $1995.
        fiatValue = Double.parseDouble(realAccount.getAssetBalance(instance.getFiat()).getFree());
        System.out.println("---Starting FIAT: " + Formatter.formatDecimal(fiatValue) + " " + instance.getFiat());
    }

    public Account getRealAccount() {
        return realAccount;
    }

    //All backend.Trade methods
    public List<Trade> getActiveTrades() {
        return activeTrades;
    }

    public List<Trade> getTradeHistory() {
        return tradeHistory;
    }

    public void setStartingValue(double startingValue) {
        this.startingValue = startingValue;
    }

    public void openTrade(Trade trade) {
        activeTrades.add(trade);
    }

    public void closeTrade(Trade trade) {
        activeTrades.remove(trade);
        tradeHistory.add(trade);
    }

    public double getFiat() {
        return fiatValue;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setFiat(double fiatValue) {
        this.fiatValue = fiatValue;
    }

    public double getTotalValue() {
        double value = 0;
        for (Map.Entry<Currency, Double> entry : wallet.entrySet()) {
            Currency currency = entry.getKey();
            Double amount = entry.getValue();
            value += amount * currency.getPrice();
        }
        return value + fiatValue;
    }

    public void addToFiat(double amount) {
        fiatValue += amount;
    }

    /**
     * Method has backend.Currency names as keys and the amount of certain currency as value.
     * i.e {"BTCUSDT : 3.23}
     *
     * @return
     */
    public ConcurrentHashMap<Currency, Double> getWallet() {
        return wallet;
    }

    /**
     * Method will calculate current profit off of all the active trades
     *
     * @return returns the sum of all the percentages wether the profit is below 0 or above.
     */
    public double getProfit() {
        return (getTotalValue() - startingValue) / startingValue;
    }


    //All wallet methods

    /**
     * Method allows to add currencies to wallet hashmap.
     *
     * @param key   Should be the name of the currency ie "BTCUSDT"
     * @param value The amount how much was bought.
     */
    public void addToWallet(Currency key, double value) {
        if (wallet.containsKey(key)) {
            wallet.put(key, wallet.get(key) + value);
        } else {
            wallet.put(key, value);
        }

    }

    /**
     * Method allows to remove values from keys.
     **/
    public void removeFromWallet(Currency key, double value) {
        wallet.put(key, wallet.get(key) - value);
    }

    public double getMakerCommission() {
        return makerCommission;
    }

    public double getTakerCommission() {
        return takerCommission;
    }

    public double getBuyerCommission() {
        return buyerCommission;
    }

    public boolean enoughFunds() {
        return nextAmount() != 0;
    }

    //Used by strategy
    public void open(Currency currency, String explanation) {
        if (currency.hasActiveTrade()) {
            System.out.println("---Cannot open trade since there already is an open trade for " + currency.getPair() + "!");
            return;
        }
        if (!enoughFunds()) {
            System.out.println("---Out of funds, cannot open trade! (" + Formatter.formatDecimal(getFiat()) + ")");
            return; //If no fiat is available, we cant trade
        }

        double currentPrice = currency.getPrice(); //Current price of the currency
        double fiatCost = nextAmount();
        double amount = fiatCost / currency.getPrice();

        Trade trade;
        if (instance.getMode().equals(Instance.Mode.LIVE)) {
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
                    + ", at a price of " + Formatter.formatDecimal(fillsPrice) + " " + instance.getFiat());
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
        addToFiat(-fiatCost);
        addToWallet(currency, amount);
        openTrade(trade);

        String message = "---" + Formatter.formatDate(trade.getOpenTime())
                + " opened trade (" + Formatter.formatDecimal(trade.getAmount()) + " "
                + currency.getPair() + "), at " + Formatter.formatDecimal(trade.getEntryPrice())
                + ", " + trade.getExplanation();
        System.out.println(message);
        if (instance.getMode().equals(Instance.Mode.BACKTESTING)) currency.appendLogLine(message);
    }

    //Used by trade
    public void close(Trade trade) {
        if (instance.getMode().equals(Instance.Mode.LIVE)) {
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
                    + ", at a price of " + Formatter.formatDecimal(fillsPrice) + " " + instance.getFiat());
            trade.setClosePrice(fillsPrice / fillsQty);
            trade.setCloseTime(order.getTransactTime());
            removeFromWallet(trade.getCurrency(), fillsQty);
            addToFiat(fillsPrice);
            System.out.println("Closed trade at an avg close of " + Formatter.formatDecimal(trade.getClosePrice()) + " ("
                    + Formatter.formatPercent((trade.getClosePrice() - trade.getCurrency().getPrice()) / trade.getClosePrice())
                    + " from current)");
        } else {
            trade.setClosePrice(trade.getCurrency().getPrice());
            trade.setCloseTime(trade.getCurrency().getCurrentTime());
            removeFromWallet(trade.getCurrency(), trade.getAmount());
            addToFiat(trade.getAmount() * trade.getClosePrice());
        }

        //Converting coin value back to fiat
        closeTrade(trade);
        trade.getCurrency().setActiveTrade(null);

        String message = "---" + (Formatter.formatDate(trade.getCloseTime())) + " closed trade ("
                + Formatter.formatDecimal(trade.getAmount()) + " " + trade.getCurrency().getPair()
                + "), at " + Formatter.formatDecimal(trade.getClosePrice())
                + ", with " + Formatter.formatPercent(trade.getProfit()) + " profit"
                + "\n------" + trade.getExplanation();
        System.out.println(message);
        if (instance.getMode().equals(Instance.Mode.BACKTESTING)) trade.getCurrency().appendLogLine(message);
    }

    private double nextAmount() {
        if (instance.getMode().equals(Instance.Mode.BACKTESTING)) return getFiat();
        return Math.min(getFiat(), getTotalValue() * instance.getConfig().getMoneyPerTrade());
    }


    //TODO: Implement limit ordering
    private NewOrderResponse placeOrder(Currency currency, double amount, boolean buy) {
        System.out.println("\n---Placing a " + (buy ? "buy" : "sell") + " market order for " + currency.getPair());
        BigDecimal originalDecimal = BigDecimal.valueOf(amount);
        //Round amount to base precision and LOT_SIZE
        int precision = client.getExchangeInfo().getSymbolInfo(currency.getPair()).getBaseAssetPrecision();
        String lotSize;
        Optional<String> minQtyOptional = client.getExchangeInfo().getSymbolInfo(currency.getPair()).getFilters().stream().filter(f -> FilterType.LOT_SIZE == f.getFilterType()).findFirst().map(f1 -> f1.getMinQty());
        Optional<String> minNotational = client.getExchangeInfo().getSymbolInfo(currency.getPair()).getFilters().stream().filter(f -> FilterType.MIN_NOTIONAL == f.getFilterType()).findFirst().map(SymbolFilter::getMinNotional);
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
