package trading;

import indicators.Indicator;
import indicators.MACD;
import indicators.RSI;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceCandlestick;
import com.webcerebrium.binance.datatype.BinanceEventKline;
import com.webcerebrium.binance.datatype.BinanceInterval;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import com.webcerebrium.binance.websocket.BinanceWebSocketAdapterKline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Currency {
    private static final String FIAT = "USDT";
    private final String coin;
    private final BinanceSymbol symbol;
    private Trade activeTrade;

    private final List<Indicator> indicators = new ArrayList<>();

    private double latestClosedPrice;
    private double currentPrice;
    private long currentTime;
    private boolean currentlyCalculating = false;

    public Currency(String coin, int historyLength, boolean trade) throws BinanceApiException {
        //Every currency is a USDT pair so we only care about the fiat opposite coin
        this.coin = coin;
        symbol = BinanceSymbol.valueOf(coin + FIAT);

        //Every needs to contain and update our indicators
        List<BinanceCandlestick> history = getCandles(historyLength);//250 gives us functionally the same accuracy as 1000
        indicators.add(new RSI(history, 14));
        indicators.add(new MACD(history, 12, 26, 9));

        //We set the initial values to check against in onMessage based on the latest candle in history
        latestClosedPrice = history.get(history.size() - 2).getClose().doubleValue();
        currentTime = history.get(history.size() - 1).getOpenTime();
        currentPrice = history.get(history.size() - 1).getClose().doubleValue();

        //We add a websocket listener that automatically updates our values and triggers our strategy or trade logic as needed
        CurrentAPI.get().websocketKlines(symbol, BinanceInterval.FIVE_MIN, new BinanceWebSocketAdapterKline() {
            @Override
            public void onMessage(BinanceEventKline message) {
                //Every message and the resulting indicator and strategy calculations is handled concurrently
                //System.out.println(Thread.currentThread().getId());

                //We want to toss messages that provide no new information
                if (currentPrice == message.getClose().doubleValue() && currentTime == message.getStartTime()) {
                    return;
                }

                currentPrice = message.getClose().doubleValue();

                //Changed candle start time means the previous candle closed and we need to update our indicators
                if (currentTime != message.getStartTime()) {
                    try {
                        //We cant use the previous currentPrice or anything else to get the closing price of the last candle, we have to check
                        latestClosedPrice = getCandles(2).get(0).getClose().doubleValue();
                    } catch (BinanceApiException e) {
                        e.printStackTrace();
                    }
                    currentTime = message.getStartTime();
                    indicators.forEach(indicator -> indicator.update(latestClosedPrice));
                }
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
                                BuySell.open(Currency.this, indicators.stream().map(indicator -> indicator.getExplanation() + "   ").collect(Collectors.joining("", "Trade opened due to: ", "")));
                            }
                        }
                    }
                    currentlyCalculating = false;
                }
            }
        });
        System.out.println("---SETUP DONE FOR " + this);
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
