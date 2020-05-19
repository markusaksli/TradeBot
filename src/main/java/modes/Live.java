package modes;

import com.webcerebrium.binance.api.BinanceApiException;
import trading.Account;
import trading.BuySell;
import trading.Currency;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class Live {
    private static Account account;
    private static List<Currency> currencies = new ArrayList<>();
    private static String[] currencyArr;

    public Live() {
        init();
    }

    public static Account getAccount() {
        return account;
    }

    public static void setCurrencyArr(String[] currencyArr) {
        Live.currencyArr = currencyArr;
    }

    public static List<Currency> getCurrencies() {
        return currencies;
    }

    private static void init() {
        Scanner sc = new Scanner(System.in);
        String apiKey;
        String apiSecret;
        while (true) {
            System.out.println("Enter your API Key: ");
            apiKey = sc.nextLine();
            if (apiKey.length() == 64) {
                System.out.println("Enter your Secret Key: ");
                apiSecret = sc.nextLine();
                if (apiSecret.length() == 64) {
                    break;
                } else System.out.println("Secret API is incorrect, enter again.");
            } else System.out.println("Incorrect API, enter again.");
        }
        account = new Account(apiKey, apiSecret);
        System.out.println(account.getMakerComission() + " Maker commission.");
        System.out.println(account.getBuyerComission() + " Buyer commission");
        System.out.println(account.getTakerComission() + " Taker comission");
        BuySell.setAccount(account);

        for (String arg : currencyArr) {
            //The currency class contains all of the method calls that drive the activity of our bot
            try {
                currencies.add(new Currency(arg));
            } catch (BinanceApiException e) {
                e.printStackTrace();
            }
        }
    }
}
