package Modes;

import com.webcerebrium.binance.api.BinanceApiException;
import trading.Account;
import trading.BuySell;
import trading.Currency;

import java.util.ArrayList;
import java.util.List;

public final class Simulation {
    private static double startingValue;
    private static String[] currencyArr;
    private static List<Currency> currencies = new ArrayList<>();
    private static Account account;

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

    public static Account getAccount() {
        return account;
    }

    private static void init() {
        account = new Account("Investor Toomas", startingValue);
        BuySell.setAccount(account);
        long startTime = System.nanoTime();

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
