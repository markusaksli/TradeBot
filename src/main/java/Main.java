import collection.PriceBean;
import collection.PriceCollector;
import com.google.gson.JsonObject;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import trading.*;
import trading.Currency;
import trading.Formatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Main {
    static List<Currency> currencies; //There should never be two of the same Currency

    public static void main(String[] args) {
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
                "-Collection is multi-threaded and thus can be CPU intensive\n" +
                "-Collected data is saved in a .txt file in the /backtesting directory\n" +
                "\n" +
                "Simulation and backtesting do not always reflect live performance\n" +
                "Make sure you are ready to commit to a strategy before starting LIVE\n");

        Scanner sc = new Scanner(System.in);
        while (true) {
            try {
                System.out.println("Enter bot mode (live, simulation, backtesting, collection)");
                Mode.set(Mode.valueOf(sc.nextLine().toUpperCase()));
                break;
            } catch (Exception e) {
                System.out.println("Invalid mode");
            }
        }
        System.out.println("---Entering " + Mode.get().name().toLowerCase() + " mode");


        if (Mode.get() == Mode.COLLECTION) {
            System.out.println("Enter collectable currency (BTC, LINK, ETH...)");
            BinanceSymbol symbol = null;
            try {
                symbol = new BinanceSymbol(sc.nextLine().toUpperCase() + "USDT");
            } catch (BinanceApiException e) {
                e.printStackTrace();
            }
            System.out.println("Enter start of collection period (Unix epoch milliseconds)");
            long start = sc.nextLong(); // March 1 00:00:00 1583020800000
            System.out.println("Enter end of collection period (Unix epoch milliseconds)");
            long end = sc.nextLong(); // March 2 00:00:00 1583107200000
            /*BinanceSymbol symbol = null;
            try {
                symbol = new BinanceSymbol("BTCUSDT");
            } catch (BinanceApiException e) {
                e.printStackTrace();
            }
            long start = 1585699200000L;
            long end = 1585710000000L;*/
            String filename = "backtesting\\" + symbol + "_" + start + "_" + end + ".txt";

            long wholePeriod = end - start;
            long toSubtract = 3 * 60 * 1000; //3 minute chunks seem most efficient and provide consistent progress.
            long chunks = wholePeriod / toSubtract; //Optimal number to reach 1200 requests per min is about 30

            PriceCollector.setRemaining(chunks);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            PriceBean.setDateFormat(dateFormat);

            final ExecutorService executorService = Executors.newCachedThreadPool();
            List<PriceCollector> collectors = new ArrayList<>();
            List<Future<?>> futures = new ArrayList<>();

            Instant initTime = Instant.now();
            for (int i = 0; i < chunks - 1; i++) {
                PriceCollector collector = new PriceCollector(end - toSubtract, end, symbol);
                collectors.add(collector);
                futures.add(executorService.submit(collector));
                end -= toSubtract;
            }
            PriceCollector finalCollector = new PriceCollector(start, end, symbol);
            collectors.add(finalCollector);
            futures.add(executorService.submit(finalCollector));
            System.out.println("---Finished creating " + collectors.size() + " chunk collectors");

            boolean done = false;
            while (!done) {
                long sinceTime = System.currentTimeMillis();
                boolean timeElapsed = true;
                while (timeElapsed) {
                    done = true;
                    for (Future<?> future : futures) {
                        done &= future.isDone();
                    }
                    timeElapsed = !done;

                    if (System.currentTimeMillis() - sinceTime > 60000) {
                        System.out.println("---"
                                + Formatter.formatDate(LocalDateTime.now())
                                + " Progress : " + Formatter.formatPercent(PriceCollector.getProgress() / chunks)
                                + ", chunks: " + (chunks - PriceCollector.getRemaining()) + "/" + chunks);
                        if (PriceCollector.getRequestPermits() > 0) {
                            System.out.println("------Bot has not used "
                                    + PriceCollector.getRequestPermits() + "/1200 requests ("
                                    + PriceCollector.getThreads() + " collectors)");
                        }
                        timeElapsed = false;
                        PriceCollector.addMinuteRequests(1200);
                    }
                }
            }
            executorService.shutdown();
            try {
                executorService.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<PriceBean> finalData = new ArrayList<>();
            collectors.stream().map(PriceCollector::getData).forEach(finalData::addAll);
            Collections.reverse(finalData);
            /*PriceBean previousDatum = finalData.get(0);
            for (int i = 0; i < finalData.size(); i++) {
                PriceBean finalDatum = finalData.get(i);
                assert finalDatum.getTimestamp() > previousDatum.getTimestamp();
                previousDatum = finalDatum;
            }
            System.out.println("---Prices are correctly ordered");*/

            System.out.println("---Collected: " + finalData.size()
                    + " aggregated trades from " + finalData.get(0).getDate()
                    + " to " + finalData.get(finalData.size() - 1).getDate()
                    + " in " + Formatter.formatDuration(Duration.between(initTime, Instant.now()))
                    + " using " + PriceCollector.getTotalRequests() + " requests");

            new File("backtesting").mkdir();
            try (FileWriter writer = new FileWriter(filename)) {
                System.out.println("---Writing file");
                writer.write(finalData.size() + " aggregated trades from "
                        + finalData.get(0).getDate()
                        + " to " + finalData.get(finalData.size() - 1).getDate()
                        + " (timestamp;price;isClosing5MinCandlePrice)\n");
                start += 300000;
                for (int i = 0; i < finalData.size(); i++) {
                    writer.write(finalData.get(i).toString() + "\n");
                    if (i < finalData.size() - 3) {
                        if (finalData.get(i + 2).getTimestamp() > start) {
                            finalData.get(i + 1).close();
                            start += 300000;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("---Collection completed, result in "
                    + filename
                    + " (" + Formatter.formatDecimal((double) new File(filename).length() / 1048576.0) + " MB)");
            System.out.println("Press enter to quit...");
            try {
                System.in.read();
            } catch (IOException ignored) {
            }
            System.out.println("...");
        } else {
            Account toomas = new Account("Investor Toomas", 1000);
            BuySell.setAccount(toomas);
            currencies = new ArrayList<>();

            long startTime = 0;
            switch (Mode.get()) {
                case LIVE:
                    if (Mode.get().equals(Mode.LIVE)) {
                        while (true) {
                            System.out.println("Enter your API Key: ");
                            String apiKey = sc.nextLine();
                            if (apiKey.length() == 64) {
                                CurrentAPI.get().setApiKey(apiKey);
                                System.out.println("Enter your Secret Key: ");
                                String apiSecret = sc.nextLine();
                                if (apiSecret.length() == 64) {
                                    CurrentAPI.get().setSecretKey(apiSecret);
                                    break;
                                } else System.out.println("Secret API is incorrect, enter again.");
                            } else System.out.println("Incorrect API, enter again.");
                        }
                        JsonObject account = null;
                        try {
                            account = CurrentAPI.get().account();
                        } catch (BinanceApiException e) {
                            e.printStackTrace();
                        }
                        //Connection with Binance API and sout-ing some info.
                        System.out.println("Maker Commission: " + account.get("makerCommission").getAsBigDecimal());
                        System.out.println("Taker Commission: " + account.get("takerCommission").getAsBigDecimal());
                        System.out.println("Buyer Commission: " + account.get("buyerCommission").getAsBigDecimal());
                        System.out.println("Seller Commission: " + account.get("sellerCommission").getAsBigDecimal());
                        System.out.println("Can Trade: " + account.get("canTrade").getAsBoolean());
                        System.out.println("Can Withdraw: " + account.get("canWithdraw").getAsBoolean());
                        System.out.println("Can Deposit: " + account.get("canDeposit").getAsBoolean());
                    }
                    break;
                case SIMULATION:
                    System.out.println("Enter all of the currencies you want to track separated with a space (BTC ETH LINK...)");
                    String[] currencyArr = sc.nextLine().toUpperCase().split(" "); //BTC ETH LINK BNB BCH XRP LTC EOS XTZ DASH ETC TRX XLM ADA ZEC
                    startTime = System.nanoTime();
                    for (String arg : currencyArr) {
                        //The currency class contains all of the method calls that drive the activity of our bot
                        try {
                            currencies.add(new Currency(arg, 250, true));
                        } catch (BinanceApiException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case BACKTESTING:
                    System.out.println("Enter backtesting data file path (absolute or relative)");
                    while (true) {
                        String path = sc.nextLine();
                        try {
                            startTime = System.nanoTime();
                            Currency currency = new Currency(path, 250);
                            currencies.add(currency);
                            int i = 0;
                            String resultPath = path.replace(".txt", "_run_" + i + ".txt");
                            while (new File(resultPath).exists()) {
                                i++;
                                resultPath = path.replace(".txt", "_run_" + i + ".txt");
                            }
                            FileWriter writer = new FileWriter(resultPath);
                            writer.write("Test ended " + Formatter.formatDate(LocalDateTime.now()) + " \n");
                            writer.write("\nTotal profit: " + Formatter.formatPercent(toomas.getProfit()) + "\n");
                            writer.write("\nActive trades:\n");
                            for (Trade trade : toomas.getActiveTrades()) {
                                writer.write(trade.toString());
                            }
                            writer.write("\nClosed trades:\n");
                            for (Trade trade : toomas.getTradeHistory()) {
                                writer.write(trade.toString());
                            }
                            writer.close();
                            System.out.println("---Simulation result file generated at " + resultPath);
                            break;
                        } catch (IOException e) {
                            System.out.println("IO failed, try again   " + e.getLocalizedMessage());
                        } catch (BinanceApiException e) {
                            System.out.println("Simulation failed, try again   " + e.getLocalizedMessage());
                        }
                    }
                    break;
            }
            long endTime = System.nanoTime();
            double time = (endTime - startTime) / 1.e9;

            System.out.println("---" + (Mode.get().equals(Mode.BACKTESTING) ? "Simulation" : "Setup") + " DONE (" + Formatter.formatDecimal(time) + " s)");

            //From this point we only use the main thread to check how the bot is doing
            System.out.println("Commands: profit, active, history, wallet, currencies, open, close, close all");
            while (true) {
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
                        System.out.println(" ");
                        break;
                    case "wallet":
                        System.out.println("Total wallet value: " + Formatter.formatDecimal(toomas.getTotalValue()) + " USDT");
                        System.out.println(toomas.getFiat() + " USDT");
                        for (Map.Entry<Currency, Double> entry : toomas.getWallet().entrySet()) {
                            if (entry.getValue() != 0) {
                                System.out.println(entry.getValue() + " " + entry.getKey().getCoin() + " (" + entry.getKey().getPrice() * entry.getValue() + " USDT)");
                            }
                        }
                        System.out.println(" ");
                        break;
                    case "currencies":
                        for (Currency currency : currencies) {
                            System.out.println((currencies.indexOf(currency) + 1) + "   " + currency);
                        }
                        System.out.println(" ");
                        break;
                    case "open":
                        System.out.println("Enter ID of currency");
                        BuySell.open(currencies.get(Integer.parseInt(sc.nextLine()) - 1), "Manually opened", Instant.now().getEpochSecond() * 1000);
                        break;
                    case "close":
                        System.out.println("Enter ID of active trade");
                        BuySell.close(toomas.getActiveTrades().get(Integer.parseInt(sc.nextLine()) - 1));
                        break;
                    case "close all":
                        toomas.getActiveTrades().forEach(BuySell::close);
                        break;
                    default:
                        System.out.println("Wrong input. Try again (profit, active, history, wallet, currencies, open, close, close all)\n");
                        break;
                }
            }
        }
    }
}
