import com.webcerebrium.binance.api.BinanceApiException;

public class Main {
    public static void main(String[] args) {
        try {
            Currency currency = new Currency("LINK");
            System.out.println(currency.getName() + " RSI= " + Indicators.getRSI(currency.getCandles(50), 14));
        } catch (
                BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        //Account toomas = new Account("Investor Toomas", 1000);
    }
}
