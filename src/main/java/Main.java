import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;
import org.knowm.xchange.bitmex.BitmexPrompt;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

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
        Currency bitcoin = new Currency("BTCUSDT");
        System.out.println(bitcoin.getPrice());
    }
}
