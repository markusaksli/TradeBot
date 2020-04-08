import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceCandlestick;
import com.webcerebrium.binance.datatype.BinanceInterval;
import com.webcerebrium.binance.datatype.BinanceSymbol;

import java.util.List;

public class Currency {
    private BinanceSymbol symbol;

    public Currency(String symbol) throws BinanceApiException {
        this.symbol = BinanceSymbol.valueOf(symbol);
    }

    public String getName() {
        return symbol.getSymbol();
    }

    public double getPrice() { //Getting the current price using Binance API
        try {
            BinanceApi ethereum = new BinanceApi();
            return (ethereum.pricesMap().get(symbol.getSymbol())).doubleValue();
        } catch (BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
            return -1;
        }
    }

    public List<BinanceCandlestick> getKlines(int lenght) throws BinanceApiException {
        return (new BinanceApi()).klines(symbol, BinanceInterval.FIVE_MIN, lenght, null);
    }
}
