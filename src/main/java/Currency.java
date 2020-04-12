import Indicators.MACD;
import Indicators.RSI;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceCandlestick;
import com.webcerebrium.binance.datatype.BinanceEventKline;
import com.webcerebrium.binance.datatype.BinanceInterval;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import com.webcerebrium.binance.websocket.BinanceWebSocketAdapterKline;

import java.util.List;

//TODO: Unique hash for HashMap?
public class Currency {
    private final BinanceSymbol symbol;
    private static final String FIAT = "USDT";
    private final RSI rsi;
    private final MACD macd;
    private double lastMACD;
    private double latestClosedPrice;
    private double currentPrice;
    private long currentTime;
    private Trade activeTrade;

    public Currency(String coin) throws BinanceApiException {
        System.out.println("Setting up scanner on " + coin + "...");
        symbol = BinanceSymbol.valueOf(coin + FIAT);
        List<BinanceCandlestick> history = getCandles(250);
        rsi = new RSI(history, 14);
        macd = new MACD(history, 12, 26, 9);
        lastMACD = macd.get();
        CurrentAPI.get().websocketKlines(symbol, BinanceInterval.FIVE_MIN, new BinanceWebSocketAdapterKline() {
            @Override
            public void onMessage(BinanceEventKline message) {
                currentPrice = message.getClose().doubleValue();
                if (currentTime == 0 || currentTime != message.getStartTime()) {
                    try {
                        latestClosedPrice = getCandles(2).get(0).getClose().doubleValue();
                    } catch (BinanceApiException e) {
                        e.printStackTrace();
                    }
                    currentTime = message.getStartTime();
                    lastMACD = macd.get();
                    rsi.update(latestClosedPrice);
                    macd.update(latestClosedPrice);
                }

                if (activeTrade != null) {
                    activeTrade.update(currentPrice);
                } else {
                    Strategy.update(Currency.this);
                }
            }
        });
        System.out.println("Setup done for " + coin);
    }

    public List<BinanceCandlestick> getCandles(int length) throws BinanceApiException {
        return (CurrentAPI.get()).klines(symbol, BinanceInterval.FIVE_MIN, length, null);
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
}
