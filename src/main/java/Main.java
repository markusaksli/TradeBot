import Indicators.IndicatorFactory;
import com.webcerebrium.binance.api.BinanceApiException;

public class Main {
    public static void main(String[] args) {
        try {
            Currency currency = new Currency("XRP");
            System.out.println(currency.getPrice());
        } catch (
                BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        //Account toomas = new Account("Investor Toomas", 1000);
    }
}
