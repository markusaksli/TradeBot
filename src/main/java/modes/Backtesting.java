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
        long startTime = System.nanoTime();
        System.out.println("Enter backtesting data file path (absolute or relative)");
        while (true) {
            String path = sc.nextLine();
            try {
                System.out.println("---Setting up...");
                startTime = System.nanoTime();
                Currency currency = new Currency(new File(path).getName().split("_")[0], path);
                currencies.add(currency);

                for (Trade trade : account.getActiveTrades()) {
                    BuySell.close(trade);
                }
                List<Trade> tradeHistory = new ArrayList<>(account.getTradeHistory());
                tradeHistory.sort(Comparator.comparingDouble(Trade::getProfit));
                System.out.println(tradeHistory);
                double maxLoss = tradeHistory.get(0).getProfit();
                double maxGain = tradeHistory.get(tradeHistory.size() - 1).getProfit();
                int lossTrades = 0;
                double lossSum = 0;
                int gainTrades = 0;
                double gainSum = 0;
                long tradeDurs = 0;
                for (Trade trade : tradeHistory) {
                    double profit = trade.getProfit();
                    if (profit < 0) {
                        lossTrades += 1;
                        lossSum += profit;
                    } else if (profit > 0) {
                        gainTrades += 1;
                        gainSum += profit;
                    }
                    tradeDurs += trade.getDuration();
                }

                double lastPrice = currency.getLastPrice();
                double firstPrice = currency.getFirstPrice();
                double maxPossible = currency.getMaxPossible();


                int i = 0;
                String resultPath = path.replace(".txt", "_run_" + i + ".txt");
                while (new File(resultPath).exists()) {
                    i++;
                    resultPath = path.replace(".txt", "_run_" + i + ".txt");
                }
                try (FileWriter writer = new FileWriter(resultPath)) {
                    writer.write("Test ended " + Formatter.formatDate(LocalDateTime.now()) + " \n");
                    //TODO: Fix max possible, right now gives way too big numbers.
                    writer.write("\nMarket performance: " + Formatter.formatPercent(((lastPrice - firstPrice) / firstPrice))
                            + ", maximum possible performance: " + Formatter.formatPercent(maxPossible / firstPrice));
                    writer.write("\nBot performance: "
                            + Formatter.formatPercent(account.getProfit()) + " from "
                            + account.getTradeHistory().size() + " closed trades with an average trade length of "
                            + Formatter.formatDuration(Duration.of(tradeDurs / tradeHistory.size(), ChronoUnit.MILLIS)) + "\n");
                    writer.write("\nLoss trades:\n");
                    writer.write(lossTrades + " trades, " + Formatter.formatPercent(lossSum / (double) lossTrades) + " average, " + Formatter.formatPercent(maxLoss) + " max");
                    writer.write("\nProfitable trades:\n");
                    writer.write(gainTrades + " trades, " + Formatter.formatPercent(gainSum / (double) gainTrades) + " average, " + Formatter.formatPercent(maxGain) + " max");
                    writer.write("\n\nClosed trades:\n");
                    for (Trade trade : tradeHistory) {
                        writer.write(trade.toString() + "\n");
                    }
                    writer.write("\nFULL LOG:\n\n");
                    writer.write(currency.getLog());
                }
                System.out.println("---Simulation result file generated at " + resultPath);
                break;
            } catch (Exception | BinanceApiException e) {
                e.printStackTrace();
                System.out.println("Testing failed, try again");
            }
        }
        while (true) {
            System.out.println("Type quit to quit");
            String s = sc.nextLine();
            if (s.toLowerCase().equals("quit")) {
                System.exit(0);
                break;
            } else {
                System.out.println("Wrong input. ");
            }
        }
    }
}
