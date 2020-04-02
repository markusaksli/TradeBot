import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;

import java.math.BigDecimal;


public class Bitcoin implements Currency {
    private String name = "BTCUSDT";
    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getPrice() {
        try {
            BinanceApi bitcoin = new BinanceApi();

            return bitcoin.pricesMap().get(getName()).doubleValue();

        } catch(BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
            return -1;
        }
    }
}
