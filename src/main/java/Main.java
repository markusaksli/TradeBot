import com.google.gson.JsonObject;
import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws BinanceApiException {
        try {
            Currency currency = new Currency("LINK");
            System.out.println(currency.getName() + " RSI= " + Indicators.getRSI(currency.getCandles(50), 14));
        } catch (
                BinanceApiException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        //Account toomas = new Account("Investor Toomas", 1000);

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter your API Key: ");
        String APIKey = sc.nextLine();
        System.out.println("Enter your Secret Key:");
        String secretKey = sc.nextLine();
        JsonObject account = (new BinanceApi(APIKey, secretKey)).account();
        //Connection with Binance API and sout-ing some info.
        System.out.println("Maker Commission: " + account.get("makerCommission").getAsBigDecimal());
        System.out.println("Taker Commission: " + account.get("takerCommission").getAsBigDecimal());
        System.out.println("Buyer Commission: " + account.get("buyerCommission").getAsBigDecimal());
        System.out.println("Seller Commission: " + account.get("sellerCommission").getAsBigDecimal());
        System.out.println("Can Trade: " +  account.get("canTrade").getAsBoolean());
        System.out.println("Can Withdraw: " + account.get("canWithdraw").getAsBoolean());
        System.out.println("Can Deposit: " + account.get("canDeposit").getAsBoolean());
    }
}