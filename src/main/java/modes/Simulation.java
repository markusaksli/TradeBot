package modes;

import com.binance.api.client.exception.BinanceApiException;
import system.ConfigSetup;
import trading.LocalAccount;
import trading.BuySell;
import trading.Currency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Simulation {
    public static double STARTING_VALUE;
    private static final List<Currency> currencies = new ArrayList<>();
    private static LocalAccount localAccount;

    private Simulation() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Currency> getCurrencies() {
        return currencies;
    }

    public static LocalAccount getAccount() {
        return localAccount;
    }

    public static void close() {
        for (Currency currency : currencies) {
            try {
                currency.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void init() {
        localAccount = new LocalAccount("Investor Toomas", STARTING_VALUE);
        BuySell.setAccount(localAccount);

        for (String arg : ConfigSetup.getCurrencies()) {
            //The currency class contains all of the method calls that drive the activity of our bot
            try {
                currencies.add(new Currency(arg));
            } catch (BinanceApiException e) {
                System.out.println("---Could not add " + arg + ConfigSetup.getFiat());
                System.out.println(e.getMessage());
            }
        }
    }
}
