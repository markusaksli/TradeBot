package trading;

import collection.PriceBean;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceCandlestick;
import com.webcerebrium.binance.datatype.BinanceEventAggTrade;
import com.webcerebrium.binance.datatype.BinanceInterval;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import com.webcerebrium.binance.websocket.BinanceWebSocketAdapterAggTrades;
import indicators.BB;
import indicators.Indicator;
import indicators.MACD;
import indicators.RSI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Currency {
    private static final String FIAT = "USDT";
    private final String coin;
    private final BinanceSymbol symbol;
    private final boolean trade;
    private Trade activeTrade;

    private final List<Indicator> indicators = new ArrayList<>();

    private final StringBuilder log = new StringBuilder();

    private double latestClosedPrice;
    private double currentPrice;
    private long candleTime;
    private long currentTime;
    private boolean currentlyCalculating = false;


    //Used for SIMULATION and LIVE
    public Currency(String coin, boolean trade) throws BinanceApiException {
        //Every currency is a USDT pair so we only care about the fiat opposite coin
        this.symbol = BinanceSymbol.valueOf(coin + FIAT);
        this.coin = coin;
        this.trade = trade;

        //Every currency needs to contain and update our indicators
        List<BinanceCandlestick> history = getCandles(250);//250 gives us functionally the same accuracy as 1000
        List<Double> closingPrices = history.stream().map(candle -> candle.getClose().doubleValue()).collect(Collectors.toList());
        indicators.add(new RSI(closingPrices, 14));
        indicators.add(new MACD(closingPrices, 12, 26, 9));
        indicators.add(new BB(closingPrices, 20));

        //We set the initial values to check against in onMessage based on the latest candle in history
        latestClosedPrice = history.get(history.size() - 2).getClose().doubleValue();
        candleTime = history.get(history.size() - 1).getOpenTime();
        currentTime = candleTime;
        currentPrice = history.get(history.size() - 1).getClose().doubleValue();

        //We add a websocket listener that automatically updates our values and triggers our strategy or trade logic as needed
        candleTime += 300000L;
        CurrentAPI.get().websocketTrades(symbol, new BinanceWebSocketAdapterAggTrades() {
            @Override
            public void onMessage(BinanceEventAggTrade message) {
                //Every message and the resulting indicator and strategy calculations is handled concurrently
                //System.out.println(Thread.currentThread().getId());

                //We want to toss messages that provide no new information
                if (currentTime == message.getPrice().doubleValue() && !(message.getEventTime() > candleTime)) {
                    return;
                }

                if (message.getEventTime() > candleTime) {
                    accept(new PriceBean(candleTime, currentPrice));
                    candleTime += 300000L;
                }

                accept(new PriceBean(message.getEventTime(), message.getPrice().doubleValue()));
            }
        });
        System.out.println("---SETUP DONE FOR " + this);
    }

    //Used for BACKTESTING
    public Currency(String pair, List<PriceBean> beans) throws BinanceApiException {
        this.symbol = BinanceSymbol.valueOf(pair);
        this.coin = pair.replace("USDT", "");
        this.trade = true;

        long start = beans.get(0).getTimestamp();
        List<BinanceCandlestick> history = getCandles(1000, start - 86400000L, start + 300000);
        List<Double> closingPrices = IntStream.range(history.size() - 251, history.size() - 1).mapToObj(history::get).map(candle -> candle.getClose().doubleValue()).collect(Collectors.toList());
        indicators.add(new RSI(closingPrices, 14));
        indicators.add(new MACD(closingPrices, 12, 26, 9));
        indicators.add(new BB(closingPrices, 20));

        System.out.println("---SETUP DONE FOR " + this);
        for (PriceBean bean : beans) {
            if (currentPrice == bean.getPrice() && !bean.isClose()) continue;
            accept(bean);
        }
    }

    private void accept(PriceBean bean) {
        currentPrice = bean.getPrice();
        currentTime = bean.getTimestamp();

        if (bean.isClose()) {
            latestClosedPrice = bean.getPrice();
            indicators.forEach(indicator -> indicator.update(latestClosedPrice));
            if (Mode.get().equals(Mode.BACKTESTING))
                appendLogLine(Formatter.formatDate(currentTime) + "  " + toString());
        }

        //Make sure we dont get concurrency issues
        if (currentlyCalculating) {
            System.out.println("------------WARNING, NEW THREAD STARTED ON " + coin + " MESSAGE DURING UNFINISHED PREVIOUS MESSAGE CALCULATIONS");
        } else {
            currentlyCalculating = true;
            if (trade) { //We can disable the strategy and trading logic to only check indicator and price accuracy
                int confluence = check();
                if (hasActiveTrade()) { //We only allow one active trade per currency, this means we only need to do one of the following:
                    activeTrade.update(currentPrice, confluence);//Update the active trade stop-loss and high values
                } else {
                    if (confluence >= 2) {
                        BuySell.open(Currency.this
                                , indicators.stream().map(indicator -> indicator.getExplanation() + "   ").collect(Collectors.joining("", "Trade opened due to: ", ""))
                                , bean.getTimestamp()
                        );
                    }
                }
            }
            currentlyCalculating = false;
        }
    }

    public int check() {
        return indicators.stream().mapToInt(indicator -> indicator.check(currentPrice)).sum();
    }

    public List<BinanceCandlestick> getCandles(int length) throws BinanceApiException {
        return (CurrentAPI.get()).klines(symbol, BinanceInterval.FIVE_MIN, length, null);
    }

    public List<BinanceCandlestick> getCandles(int length, long start, long end) throws BinanceApiException {
        Map<String, Long> options = new HashMap<>();
        options.put("startTime", start);
        options.put("endTime", end);
        return (CurrentAPI.get()).klines(symbol, BinanceInterval.FIVE_MIN, length, options);
    }

    public String getCoin() {
        return coin;
    }

    public double getPrice() {
        return currentPrice;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public boolean hasActiveTrade() {
        return activeTrade != null;
    }

    public void setActiveTrade(Trade activeTrade) {
        this.activeTrade = activeTrade;
    }

    public String getLog() {
        return log.toString();
    }

    public void appendLogLine(String s) {
        log.append(s).append("\n");
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(coin + " price: " + currentPrice);
        if (currentTime == candleTime)
            indicators.forEach(indicator -> s.append(", ").append(indicator.getClass().getSimpleName()).append(": ").append(Formatter.formatDecimal(indicator.get())));
        else
            indicators.forEach(indicator -> s.append(", ").append(indicator.getClass().getSimpleName()).append(": ").append(Formatter.formatDecimal(indicator.getTemp(currentPrice))));
        s.append(", hasActive: ").append(hasActiveTrade()).append(")");
        return s.toString();
    }

    @Override
    public int hashCode() {
        return coin.hashCode();
    }
}
