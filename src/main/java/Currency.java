import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;

public class Currency {
    private String name;

    public Currency(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

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
