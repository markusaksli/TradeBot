import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;

public class Main {
    public static void main(String[] args) {
        try {
            BinanceApi api = new BinanceApi();

            System.out.println("ETH-BTC PRICE=" + api.pricesMap().get("ETHBTC"));
        } catch (
                BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
