package experimental;

import collection.PriceBean;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.*;
import com.webcerebrium.binance.websocket.BinanceWebSocketAdapterAggTrades;
import com.webcerebrium.binance.websocket.BinanceWebSocketAdapterKline;
import indicators.BB;
import indicators.Indicator;
import indicators.MACD;
import indicators.RSI;
import trading.CurrentAPI;
import trading.Formatter;
import trading.Trade;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ComparisonCurrency {
    private static final String FIAT = "USDT";
    private final String coin;
    private final BinanceSymbol symbol;
    private Trade activeTrade;

    private final List<Indicator> indicators = new ArrayList<>();

    private StringBuilder log = new StringBuilder();
    private StringBuilder rawLog = new StringBuilder();
    private PriceBean lastBean;

    private double latestClosedPrice;
    private double currentPrice;
    private long candleTime;
    private long currentTime;


    public ComparisonCurrency(String coin, boolean klines) throws BinanceApiException {
        //Every currency is a USDT pair so we only care about the fiat opposite coin
        this.symbol = BinanceSymbol.valueOf(coin + FIAT);
        this.coin = coin;

        //Every currency needs to contain and update our indicators
        List<BinanceCandlestick> history = getCandles(250);//250 gives us functionally the same accuracy as 1000
        List<Double> closingPrices = history.stream().map(candle -> candle.getClose().doubleValue()).collect(Collectors.toList());
        indicators.add(new RSI(closingPrices, 14));
        indicators.add(new MACD(closingPrices, 12, 26, 9));

        //We set the initial values to check against in onMessage based on the latest candle in history
        latestClosedPrice = history.get(history.size() - 2).getClose().doubleValue();
        candleTime = history.get(history.size() - 1).getOpenTime();
        currentTime = candleTime;
        currentPrice = history.get(history.size() - 1).getClose().doubleValue();
        lastBean = new PriceBean(currentTime, currentPrice);

        //We add a websocket listener that automatically updates our values and triggers our strategy or trade logic as needed

        if (klines) {
            CurrentAPI.get().websocketKlines(symbol, BinanceInterval.FIVE_MIN, new BinanceWebSocketAdapterKline() {
                @Override
                public void onMessage(BinanceEventKline message) {
                    //Every message and the resulting indicator and strategy calculations is handled concurrently
                    //System.out.println(Thread.currentThread().getId());

                    //We want to toss messages that provide no new information
                    if (currentPrice == message.getClose().doubleValue() && candleTime == message.getStartTime()) {
                        return;
                    }
                    PriceBean priceBean = new PriceBean(message.getEventTime(), message.getClose().doubleValue());

                    //Changed candle start time means the previous candle closed and we need to update our indicators
                    if (candleTime != message.getStartTime()) {
                        try {
                            BinanceCandlestick closeStick = getCandles(2).get(0);
                            accept(new PriceBean(closeStick.getCloseTime(), closeStick.getClose().doubleValue(), 1));
                            candleTime = message.getStartTime();
                        } catch (BinanceApiException e) {
                            e.printStackTrace();
                        }
                    }
                    accept(priceBean);
                }
            });
        } else {
            candleTime += 300000L;
            CurrentAPI.get().websocketTrades(symbol, new BinanceWebSocketAdapterAggTrades() {
                @Override
                public void onMessage(BinanceEventAggTrade message) {
                    //Every message and the resulting indicator and strategy calculations is handled concurrently
                    //System.out.println(Thread.currentThread().getId());

                    //We want to toss messages that provide no new information
                    if (lastBean.getPrice() == message.getPrice().doubleValue()) {
                        return;
                    }

                    if (message.getEventTime() > candleTime) {
                        lastBean.close();
                        candleTime += 300000L;
                    }
                    accept(lastBean);
                    lastBean = new PriceBean(message.getEventTime(), message.getPrice().doubleValue());

                    while (candleTime < currentTime) {
                        candleTime += 300000L;
                    }
                }
            });
        }
        System.out.println("---SETUP DONE FOR " + this);
    }

    public ComparisonCurrency(String filename) throws BinanceApiException, IOException {
        this.symbol = BinanceSymbol.valueOf(new File(filename).getName().split("_")[0]);
        this.coin = symbol.toString().replace("USDT", "");

        List<PriceBean> beans = Formatter.formatData(filename);
        long start = beans.get(0).getTimestamp();
        List<BinanceCandlestick> history = getCandles(1000, start - 86400000L, start + 300000);
        List<Double> closingPrices = IntStream.range(history.size() - 251, history.size() - 1).mapToObj(history::get).map(candle -> candle.getClose().doubleValue()).collect(Collectors.toList());
        indicators.add(new RSI(closingPrices, 14));
        indicators.add(new MACD(closingPrices, 12, 26, 9));
        indicators.add(new BB(closingPrices, 20));
        lastBean = beans.get(0);

        for (PriceBean bean : beans) {
            if (bean.getPrice() == lastBean.getPrice() && !bean.isClose()) continue;
            accept(lastBean);
            lastBean = bean;
        }
    }

    private void accept(PriceBean bean) {

        currentPrice = bean.getPrice();
        currentTime = bean.getTimestamp();


        if (bean.isClose()) {
            latestClosedPrice = bean.getPrice();
            indicators.forEach(indicator -> indicator.update(latestClosedPrice));
            System.out.println(toString());
            appendLogLine("---" + toString());
        }

        appendRawLogLine(bean.toString());
        appendLogLine(toString());
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

    public double getPrice() { //Getting the current price using Binance API
        return currentPrice;
    }

    public long getCandleTime() {
        return candleTime;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public BinanceSymbol getSymbol() {
        return symbol;
    }

    public double getLatestClosedPrice() {
        return latestClosedPrice;
    }

    public boolean hasActiveTrade() {
        return activeTrade != null;
    }

    public Trade getActiveTrade() {
        return activeTrade;
    }

    public void setActiveTrade(Trade activeTrade) {
        this.activeTrade = activeTrade;
    }

    public String getLog() {
        return log.toString();
    }

    public String getRawLog() {
        return rawLog.toString();
    }

    public void appendLogLine(String s) {
        log.append(s).append("\n");
    }

    public void appendRawLogLine(String s) {
        rawLog.append(s).append("\n");
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(coin + "  " + Formatter.formatDate(currentTime) + " price: " + currentPrice);
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
