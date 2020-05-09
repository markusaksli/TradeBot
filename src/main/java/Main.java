import Modes.Backtesting;
import Modes.Collection;
import Modes.Live;
import Modes.Simulation;
import collection.ConfigSetup;
import org.apache.logging.log4j.LogManager;
import trading.Currency;
import trading.Formatter;
import trading.*;

import java.io.FileWriter;
import java.io.IOException;
import java.time.*;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Main {
    private static List<Currency> currencies;
    //private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        //Program config.
        new ConfigSetup();

        System.out.println("Welcome to TradeBot\n" +
                "(made by Markus Aksli, Marten Türk, and Mark Robin Kalder)\n" +
                "\n" +
                "This is a cryptocurrency trading bot that uses the Binance API,\n" +
                "and a strategy based on a couple of 5 minute chart indicators\n" +
                "(RSI, MACD, Bollinger Bands)\n" +
                "(The bot only trades USDT fiat pairs)\n" +
                "\n" +
                "The bot has the following modes of operation:\n" +
                "---LIVE\n" +
                "-This mode trades with real money on the Binance platform\n" +
                "---SIMULATION\n" +
                "-Real-time trading simulation based on actual market data\n" +
                "-Trades are only simulated based on market prices \n" +
                "-No actual orders are made\n" +
                "---BACKTESTING\n" +
                "-Simulation based on historical data.\n" +
                "-Allows for quick testing of the behavior and profitability of the bot\n" +
                "-Data needs to be loaded from a file created with the COLLECTION mode\n" +
                "---COLLECTION\n" +
                "-Collects raw market price data from a specified time period\n" +
                "-Collection is multi-threaded and can be CPU and memory intensive\n" +
                "-Collected data is saved in a file in the /backtesting directory\n" +
                "\n" +
                "Simulation and backtesting do not always reflect live performance\n" +
                "Make sure you are ready to commit to a strategy before starting LIVE\n");
        //Tester.
        //logger.debug("Hello");
        Scanner sc = new Scanner(System.in);
        while (true) {
            try {
                System.out.println("Enter bot mode (live, simulation, backtesting, collection)");
                Mode.set(Mode.valueOf(sc.nextLine().toUpperCase()));
                break;
            } catch (Exception e) {
                LogManager.getRootLogger().error("Invalid mode, try again.");
            }
        }
        System.out.println("---Entering " + Mode.get().name().toLowerCase() + " mode");


        if (Mode.get() == Mode.COLLECTION) {
            new Collection(); //Init collection mode.

        } else {
            Account toomas = null;
            long startTime = 0;
            switch (Mode.get()) {
                case LIVE:
                    new Live(); //Init live mode.
                    toomas = Live.getAccount();

                    break;
                case SIMULATION:
                    new Simulation(); //Init simulation mode.
                    currencies = Simulation.getCurrencies();
                    toomas = Simulation.getAccount();
                    break;
                case BACKTESTING:
                    new Backtesting(); //Init Backtesting mode.
                    currencies = Backtesting.getCurrencies();
                    toomas = Backtesting.getAccount();
                    break;
            }
            long endTime = System.nanoTime();
            double time = (endTime - startTime) / 1.e9;

            System.out.println("---" + (Mode.get().equals(Mode.BACKTESTING) ? "Simulation" : "Setup") + " DONE (" + Formatter.formatDecimal(time) + " s)");

            //From this point we only use the main thread to check how the bot is doing
            while (true) {
                System.out.println("Commands: profit, active, history, wallet, currencies, open, close, close all, log, quit");
                String in = sc.nextLine();
                switch (in) {
                    case "profit":
                        System.out.println("Account profit: " + Formatter.formatPercent(toomas.getProfit()) + "\n");
                        break;
                    case "active":
                        System.out.println("Active trades:");
                        for (Trade trade : toomas.getActiveTrades()) {
                            System.out.println(trade);
                        }
                        System.out.println(" ");
                        break;
                    case "history":
                        System.out.println("Closed trades:");
                        for (Trade trade : toomas.getTradeHistory()) {
                            System.out.println(trade);
                        }
                        break;
                    case "wallet":
                        System.out.println("Total wallet value: " + Formatter.formatDecimal(toomas.getTotalValue()) + " USDT");
                        System.out.println(toomas.getFiat() + " USDT");
                        for (Map.Entry<Currency, Double> entry : toomas.getWallet().entrySet()) {
                            if (entry.getValue() != 0) {
                                System.out.println(entry.getValue() + " " + entry.getKey().getCoin() + " (" + entry.getKey().getPrice() * entry.getValue() + " USDT)");
                            }
                        }
                        break;
                    case "currencies":
                        for (Currency currency : Simulation.getCurrencies()) {
                            System.out.println((Simulation.getCurrencies().indexOf(currency) + 1) + "   " + currency);
                        }
                        System.out.println(" ");
                        break;
                    case "open":
                        System.out.println("Enter ID of currency");
                        BuySell.open(currencies.get(Integer.parseInt(sc.nextLine()) - 1), "Manually opened", Instant.now().getEpochSecond() * 1000);
                        break;
                    case "close":
                        System.out.println("Enter ID of active trade");
                        List<Trade> accTrades = toomas.getActiveTrades();
                        String tradeId = sc.nextLine();
                        if (accTrades.contains(tradeId))
                            BuySell.close(toomas.getActiveTrades().get(Integer.parseInt(tradeId) - 1));
                        break;
                    case "close all":
                        toomas.getActiveTrades().forEach(BuySell::close);
                        break;
                    case "log":
                        List<Trade> tradeHistory = new ArrayList<>(toomas.getTradeHistory());
                        if (tradeHistory.isEmpty()) {
                            System.out.println("---No closed trades yet");
                            continue;
                        }
                        tradeHistory.sort(Comparator.comparingDouble(Trade::getProfit));
                        double maxLoss = tradeHistory.get(0).getProfit();
                        double maxGain = tradeHistory.get(tradeHistory.size() - 1).getProfit();
                        int lossTrades = 0;
                        double lossSum = 0;
                        int gainTrades = 0;
                        double gainSum = 0;
                        for (Trade trade : tradeHistory) {
                            double profit = trade.getProfit();
                            if (profit < 0) {
                                lossTrades += 1;
                                lossSum += profit;
                            } else if (profit > 0) {
                                gainTrades += 1;
                                gainSum += profit;
                            }
                        }
                        try (FileWriter writer = new FileWriter("log.txt")) {
                            writer.write("Test ended " + Formatter.formatDate(LocalDateTime.now()) + " \n");
                            writer.write("\nTotal profit: " + Formatter.formatPercent(toomas.getProfit()) + " from " + toomas.getTradeHistory().size() + " closed trades\n");
                            writer.write("\nLoss trades:\n");
                            writer.write(lossTrades + " trades, " + Formatter.formatPercent(lossSum / (double) lossTrades) + " average, " + Formatter.formatPercent(maxLoss) + " max");
                            writer.write("\nProfitable trades:\n");
                            writer.write(gainTrades + " trades, " + Formatter.formatPercent(gainSum / (double) gainTrades) + " average, " + Formatter.formatPercent(maxGain) + " max");
                            writer.write("\nActive trades:\n");
                            for (Trade trade : toomas.getActiveTrades()) {
                                writer.write(trade.toString() + "\n");
                            }
                            writer.write("\nClosed trades:\n");
                            for (Trade trade : tradeHistory) {
                                writer.write(trade.toString() + "\n");
                            }
                            System.out.println("---Created log in log.txt");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "quit":
                        System.exit(0);
                    default:
                        LogManager.getRootLogger().error("Wrong input. Try again. ");
                        break;
                }
            }
        }
    }

}

