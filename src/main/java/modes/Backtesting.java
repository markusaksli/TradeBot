package modes;

import trading.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class Backtesting {
    private static final List<Currency> currencies = new ArrayList<>();
    private static LocalAccount localAccount;

    private Backtesting() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Currency> getCurrencies() {
        return currencies;
    }

    public static LocalAccount getAccount() {
        return localAccount;
    }

    public static void startBacktesting() {
        final String[] backtestingFiles = Collection.getDataFiles();
        if (backtestingFiles.length == 0) {
            System.out.println("No backtesting files detected!");
            System.exit(0);
        }
        localAccount = new LocalAccount("Investor Toomas", Simulation.STARTING_VALUE);
        BuySell.setAccount(localAccount);
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\nBacktesting data files:\n");
            for (int i = 0; i < backtestingFiles.length; i++) {
                System.out.println("[" + (i + 1) + "] " + backtestingFiles[i]);
            }
            System.out.println("\nEnter a number to select the backtesting data file");
            String input = sc.nextLine();
            if (!input.matches("\\d+")) continue;
            int index = Integer.parseInt(input);
            if (index > backtestingFiles.length) {
                continue;
            }
            String path = "backtesting/" + backtestingFiles[index - 1];
            try {
                System.out.println("\n---Setting up...");
                Currency currency = new Currency(new File(path).getName().split("_")[0], path);
                currencies.add(currency);

                for (Trade trade : localAccount.getActiveTrades()) {
                    trade.setExplanation(trade.getExplanation() + "Manually closed");
                    BuySell.close(trade);
                }


                int i = 1;
                path = path.replace("backtesting", "log");
                String resultPath = path.replace(".dat", "_run_" + i + ".txt");
                while (new File(resultPath).exists()) {
                    i++;
                    resultPath = path.replace(".dat", "_run_" + i + ".txt");
                }
                new File("log").mkdir();

                currency.log(resultPath);
                break;
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Testing failed, try again");
            }
        }
    }
}
