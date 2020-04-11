import Indicators.RSI;
import com.webcerebrium.binance.api.BinanceApiException;

public class Main {
    public static void main(String[] args) {
        try {
            Currency currency = new Currency("LINK");
            System.out.println(currency.getPrice());
            RSI rsi = new RSI(currency.getCandles(1000), 14);
            System.out.println(rsi.getTemp(currency.getPrice()));
        } catch (
                BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        //Account toomas = new Account("Investor Toomas", 1000);
    }
}
