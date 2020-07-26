package modes;

import trading.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//TODO: Clean up Backtesting class.
public final class Backtesting {
    private static double startingValue;
    private static final List<Currency> currencies = new ArrayList<>();
    private static LocalAccount localAccount;

    public Backtesting() {
        init();
    }

    public static void setStartingValue(double startingValue) {
        Backtesting.startingValue = startingValue;
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
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter backtesting data file path (absolute or relative)");
        while (true) {
            String path = sc.nextLine();
            try {
                System.out.println("---Setting up...");
                Currency currency = new Currency(new File(path).getName().split("_")[0], path);
                currencies.add(currency);

                for (Trade trade : localAccount.getActiveTrades()) {
                    trade.setExplanation(trade.getExplanation() + "Manually closed");
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
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Testing failed, try again");
            }
        }
    }
}
