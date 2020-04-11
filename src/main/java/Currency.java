import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceCandlestick;
import com.webcerebrium.binance.datatype.BinanceInterval;
import com.webcerebrium.binance.datatype.BinanceSymbol;

import java.util.List;

public class Currency {
    private final BinanceSymbol symbol;
    private static final String FIAT = "USDT";

    public Currency(String symbol) throws BinanceApiException {
        this.symbol = BinanceSymbol.valueOf(symbol + FIAT);
    }

    public String getName() {
        return symbol.getSymbol();
    }

    public BinanceSymbol getSymbol() {
        return symbol;
    }

    public double getPrice() { //Getting the current price using Binance API
        try {
            return (CurrentAPI.get().pricesMap().get(symbol.getSymbol())).doubleValue();
        } catch (BinanceApiException e) {

            System.out.println("ERROR: " + e.getMessage());
            return -1;
        }
    }

    public List<BinanceCandlestick> getCandles(int lenght) throws BinanceApiException {
        return (CurrentAPI.get()).klines(symbol, BinanceInterval.FIVE_MIN, lenght, null);
    }

    //TODO: Unique hash for HashMap?
}
