import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;

import java.math.BigDecimal;

public class Ethereum implements Currency{
    private String name = "ETHBTC";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getPrice() { //Getting the current price using Binance API
        try {
            BinanceApi ethereum = new BinanceApi();
            return (ethereum.pricesMap().get(getName())).doubleValue();
        } catch(BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
            return -1;
        }
    }
}
