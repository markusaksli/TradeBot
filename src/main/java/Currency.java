import Indicators.MACD;
import Indicators.RSI;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceCandlestick;
import com.webcerebrium.binance.datatype.BinanceEventKline;
import com.webcerebrium.binance.datatype.BinanceInterval;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import com.webcerebrium.binance.websocket.BinanceWebSocketAdapterKline;

import java.util.List;

public class Currency {
    private static final String FIAT = "USDT";
    private final String coin;
    private final BinanceSymbol symbol;
    private Trade activeTrade;

    private final RSI rsi;
    private final MACD macd;
    private double lastMACD;

    private double latestClosedPrice;
    private double currentPrice;
    private long currentTime;

    public Currency(String coin, int historyLength, boolean trade) throws BinanceApiException {
        //Every currency is a USDT pair so we only care about the fiat opposite coin
        this.coin = coin;
        symbol = BinanceSymbol.valueOf(coin + FIAT);

        //Every needs to contain and update our indicators
        List<BinanceCandlestick> history = getCandles(historyLength);//250 gives us functionally the same accuracy as 1000
        rsi = new RSI(history, 14);
        macd = new MACD(history, 12, 26, 9);
        lastMACD = macd.get();

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
                    //System.out.println("Message ignored");
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
                    lastMACD = macd.get();
                    rsi.update(latestClosedPrice);
                    macd.update(latestClosedPrice);
                }

                if (trade) { //We can disable the strategy and trading logic to only check indicator and price accuracy
                    if (activeTrade != null) { //We only allow one active trade per currency, this means we only need to do one of the following:
                        activeTrade.update(currentPrice);//Update the active trade stop-loss and high values
                    } else {
                        Strategy.update(Currency.this);//Check for a buy signal to open a new trade
                    }
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

    public RSI getRsi() {
        return rsi;
    }

    public MACD getMacd() {
        return macd;
    }

    public double getLastMACD() {
        return lastMACD;
    }

    public double getLatestClosedPrice() {
        return latestClosedPrice;
    }

    public Trade getActiveTrade() {
        return activeTrade;
    }

    public void setActiveTrade(Trade activeTrade) {
        this.activeTrade = activeTrade;
    }

    @Override
    public String toString() {
        return coin + " (price: " + currentPrice + ", RSI: " + Formatter.formatDecimal(rsi.getTemp(currentPrice)) + ", MACD: " + Formatter.formatDecimal(macd.getTemp(currentPrice)) + ", hasActive: " + (activeTrade != null) + ")";
    }

    @Override
    public int hashCode() {
        return coin.hashCode();
    }
}
