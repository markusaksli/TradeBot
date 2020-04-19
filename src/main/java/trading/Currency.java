package trading;

import collection.PriceBean;
import com.webcerebrium.binance.datatype.*;
import com.webcerebrium.binance.websocket.BinanceWebSocketAdapterAggTrades;
import indicators.BB;
import indicators.Indicator;
import indicators.MACD;
import indicators.RSI;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.websocket.BinanceWebSocketAdapterKline;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Currency {
    private static final String FIAT = "USDT";
    private final String coin;
    private final BinanceSymbol symbol;
    private final boolean trade;
    private Trade activeTrade;

    private final List<Indicator> indicators = new ArrayList<>();

    private double latestClosedPrice;
    private double currentPrice;
    private long candleTime;
    private long currentTime;
    private boolean currentlyCalculating = false;

    //Used for SIMULATION and LIVE
    public Currency(String coin, int historyLength, boolean trade) throws BinanceApiException {
        //Every currency is a USDT pair so we only care about the fiat opposite coin
        this.symbol = BinanceSymbol.valueOf(coin + FIAT);
        this.coin = coin;
        this.trade = trade;

        //Every currency needs to contain and update our indicators
        List<BinanceCandlestick> history = getCandles(historyLength);//250 gives us functionally the same accuracy as 1000
        List<Double> closingPrices = history.stream().map(candle -> candle.getClose().doubleValue()).collect(Collectors.toList());
        indicators.add(new RSI(closingPrices, 14));
        indicators.add(new MACD(closingPrices, 12, 26, 9));
        indicators.add(new BB(closingPrices, 20));

        //We set the initial values to check against in onMessage based on the latest candle in history
        latestClosedPrice = history.get(history.size() - 2).getClose().doubleValue();
        candleTime = history.get(history.size() - 1).getOpenTime();
        currentPrice = history.get(history.size() - 1).getClose().doubleValue();

        //We add a websocket listener that automatically updates our values and triggers our strategy or trade logic as needed

        CurrentAPI.get().websocketKlines(symbol, BinanceInterval.FIVE_MIN, new BinanceWebSocketAdapterKline() {
            @Override
            public void onMessage(BinanceEventKline message) {
                if (currentPrice == message.getClose().doubleValue() && candleTime == message.getStartTime()) {
                    return;
                }
                PriceBean priceBean = new PriceBean(message.getEndTime(), message.getClose().doubleValue());
                if (candleTime != message.getStartTime()) {
                    try {
                        BinanceCandlestick closeStick = getCandles(2).get(0);
                        accept(new PriceBean(closeStick.getCloseTime(), closeStick.getClose().doubleValue(), 1));
                        candleTime = closeStick.getCloseTime();
                    } catch (BinanceApiException e) {
                        e.printStackTrace();
                    }
                }
                accept(priceBean);
            }
        });
        System.out.println("---SETUP DONE FOR " + this);
    }

    public Currency(String filename, int historyLength) throws BinanceApiException, IOException {
        this.symbol = BinanceSymbol.valueOf(new File(filename).getName().split("_")[0]);
        this.coin = symbol.toString().replace("USDT", "");
        this.trade = true;

        List<PriceBean> beans = Formatter.formatData(filename);
        int historyIndex = 0;
        List<PriceBean> history = new ArrayList<>();
        for (int i = 0; i < beans.size() - 1; i++) {
            if (beans.get(i).isClose()) {
                PriceBean bean = beans.get(i);
                history.add(bean);
                if (history.size() == historyLength) {
                    historyIndex = i + 1;
                    break;
                }
            }
        }
        List<Double> closingPrices = history.stream().map(PriceBean::getPrice).collect(Collectors.toList());
        indicators.add(new RSI(closingPrices, 14));
        indicators.add(new MACD(closingPrices, 12, 26, 9));
        indicators.add(new BB(closingPrices, 20));

        for (int i = historyIndex; i < beans.size(); i++) {
            accept(beans.get(i));
        }
    }

    private void accept(PriceBean bean) {
        //Every message and the resulting indicator and strategy calculations is handled concurrently
        //System.out.println(Thread.currentThread().getId());

        //We want to toss messages that provide no new information


        currentPrice = bean.getPrice();
        currentTime = bean.getTimestamp();

        if (bean.isClose()) {
            latestClosedPrice = bean.getPrice();
            indicators.forEach(indicator -> indicator.update(latestClosedPrice));
        }
        //Changed candle start time means the previous candle closed and we need to update our indicators
        //Make sure we dont get concurrency issues
        if (currentlyCalculating) {
            System.out.println("------------WARNING, NEW THREAD STARTED ON " + coin + " MESSAGE DURING UNFINISHED PREVIOUS MESSAGE CALCULATIONS");
        } else {
            currentlyCalculating = true;
            if (trade) { //We can disable the strategy and trading logic to only check indicator and price accuracy
                if (hasActiveTrade()) { //We only allow one active trade per currency, this means we only need to do one of the following:
                    activeTrade.update(currentPrice);//Update the active trade stop-loss and high values
                } else {
                    if (indicators.stream().mapToInt(indicator -> indicator.check(currentPrice)).sum() >= 2) {
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

    public List<BinanceCandlestick> getCandles(int length) throws BinanceApiException {
        return (CurrentAPI.get()).klines(symbol, BinanceInterval.FIVE_MIN, length, null);
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

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(coin + " (price: " + currentPrice);
        for (Indicator indicator : indicators) {
            s.append(", ").append(indicator.getClass().getSimpleName()).append(": ").append(Formatter.formatDecimal(indicator.getTemp(currentPrice)));
        }
        s.append(", hasActive: ").append(hasActiveTrade()).append(")");
        return s.toString();
    }

    @Override
    public int hashCode() {
        return coin.hashCode();
    }
}
