package modes;

import trading.LocalAccount;
import trading.BuySell;
import trading.Currency;

import java.util.ArrayList;
import java.util.List;

//TODO: Clean up Simulation class
public final class Simulation {
    private static double startingValue;
    private static String[] currencyArr;
    private static final List<Currency> currencies = new ArrayList<>();
    private static LocalAccount localAccount;

    public Simulation() {
        init();
    }

    public static void setCurrencyArr(String[] currencyArr) {
        Simulation.currencyArr = currencyArr;
    }

    public static void setStartingValue(double startingValue) {
        Simulation.startingValue = startingValue;
    }

    public static List<Currency> getCurrencies() {
        return currencies;
    }

    public static LocalAccount getAccount() {
        return localAccount;
    }

    private static void init() {
        localAccount = new LocalAccount("Investor Toomas", startingValue);
        BuySell.setAccount(localAccount);

        for (String arg : currencyArr) {
            //The currency class contains all of the method calls that drive the activity of our bot
            currencies.add(new Currency(arg));
        }
    }
}
