import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;

public class Main {
    public static void main(String[] args) {
        try {
            BinanceApi api = new BinanceApi();

            System.out.println("BTC-USD PRICE=" + api.pricesMap().get("BTCUSDT"));
        } catch (
                BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
