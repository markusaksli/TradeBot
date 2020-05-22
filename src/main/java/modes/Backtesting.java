package modes;

import com.webcerebrium.binance.api.BinanceApiException;
import trading.*;

import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public final class Backtesting {
    private static double startingValue;
    private static List<Currency> currencies = new ArrayList<>();
    private static Account account;

    public Backtesting() {
        init();
    }

    public static void setStartingValue(double startingValue) {
        Backtesting.startingValue = startingValue;
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
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter backtesting data file path (absolute or relative)");
        while (true) {
            String path = sc.nextLine();
            try {
                System.out.println("---Setting up...");
                Currency currency = new Currency(new File(path).getName().split("_")[0], path);
                currencies.add(currency);

                for (Trade trade : account.getActiveTrades()) {
                    BuySell.close(trade);
                }

                int i = 1;
                String resultPath = path.replace(".dat", "_run_" + i + ".txt");
                while (new File(resultPath).exists()) {
                    i++;
                    resultPath = path.replace(".dat", "_run_" + i + ".txt");
                }

                currency.log(resultPath);
                break;
            } catch (Exception | BinanceApiException e) {
                e.printStackTrace();
                System.out.println("Testing failed, try again");
            }
        }
    }
}
