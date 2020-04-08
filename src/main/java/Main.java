import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;



public class Main {
    public static void main(String[] args) {
        try {
            BinanceApi api = new BinanceApi();

            System.out.println("ETH-BTC PRICE=" + api.pricesMap().get("ETHBTC").doubleValue());
        } catch (
                BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        //Account toomas = new Account("Investor Toomas", 1000);
        Currency bitcoin = null;
        try {
            bitcoin = new Currency("BTCUSDT");
        } catch (BinanceApiException e) {
            e.printStackTrace();
        }
        System.out.println(bitcoin.getPrice());
    }
}
