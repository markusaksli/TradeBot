package modes;

import trading.LocalAccount;
import trading.BuySell;
import trading.Currency;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//TODO: Clean up Live class
public final class Live {
    private static LocalAccount localAccount;
    private static final List<Currency> currencies = new ArrayList<>();
    private static String[] currencyArr;

    private Live() {
        throw new IllegalStateException("Utility class");
    }

    public static LocalAccount getAccount() {
        return localAccount;
    }

    public static void setCurrencyArr(String[] currencyArr) {
        Live.currencyArr = currencyArr;
    }

    public static List<Currency> getCurrencies() {
        return currencies;
    }

    public static void init() {
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
        localAccount = new LocalAccount(apiKey, apiSecret);
        System.out.println(localAccount.getMakerComission() + " Maker commission.");
        System.out.println(localAccount.getBuyerComission() + " Buyer commission");
        System.out.println(localAccount.getTakerComission() + " Taker comission");
        BuySell.setAccount(localAccount);

        //TODO: Differentiate setup between currencies already present in wallet and those that are not
        for (String arg : currencyArr) {
            //The currency class contains all of the method calls that drive the activity of our bot
            currencies.add(new Currency(arg));
        }
    }
}
