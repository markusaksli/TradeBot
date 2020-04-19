import collection.PriceBean;
import collection.TradeCollector;
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
    static Set<Currency> currencies; //There should never be two of the same Currency

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
                "---SIMULATED\n" +
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
                System.out.println("Enter bot mode (live, simulated, backtesting, collection)");
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
            System.out.println("Press enter to start collecting backtesting data...");
            try {
                System.in.read();
            } catch (IOException ignored) {
            }
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

            TradeCollector.setRemaining(chunks);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            PriceBean.setDateFormat(dateFormat);

            final ExecutorService executorService = Executors.newCachedThreadPool();
            List<TradeCollector> collectors = new ArrayList<>();
            List<Future<?>> futures = new ArrayList<>();

            Instant initTime = Instant.now();
            for (int i = 0; i < chunks - 1; i++) {
                TradeCollector collector = new TradeCollector(end - toSubtract, end, symbol);
                collectors.add(collector);
                futures.add(executorService.submit(collector));
                end -= toSubtract;
            }
            TradeCollector finalCollector = new TradeCollector(start, end, symbol);
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
                                + " Overall progress : " + Formatter.formatPercent(TradeCollector.getProgress() / chunks)
                                + ", remaining chunks: " + TradeCollector.getRemaining() + "/" + chunks);
                        if (TradeCollector.getRequestPermits() > 0) {
                            System.out.println("------Bot has not used "
                                    + TradeCollector.getRequestPermits() + "/1200 requests ("
                                    + TradeCollector.getThreads() + " collectors)");
                        }
                        timeElapsed = false;
                        TradeCollector.addMinuteRequests(1200);
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
            collectors.stream().map(TradeCollector::getData).forEach(finalData::addAll);
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
                    + " using " + TradeCollector.getTotalRequests() + " requests");

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

        /*JsonObject account = CurrentAPI.get().account();
        //Connection with Binance API and sout-ing some info.
        System.out.println("Maker Commission: " + account.get("makerCommission").getAsBigDecimal());
        System.out.println("Taker Commission: " + account.get("takerCommission").getAsBigDecimal());
        System.out.println("Buyer Commission: " + account.get("buyerCommission").getAsBigDecimal());
        System.out.println("Seller Commission: " + account.get("sellerCommission").getAsBigDecimal());
        System.out.println("Can Trade: " + account.get("canTrade").getAsBoolean());
        System.out.println("Can Withdraw: " + account.get("canWithdraw").getAsBoolean());
        System.out.println("Can Deposit: " + account.get("canDeposit").getAsBoolean());*/

            currencies = new HashSet<>(); //BTC ETH LINK BNB BCH XRP LTC EOS XTZ DASH ETC TRX XLM ADA ZEC
            long startTime = System.nanoTime();
            System.out.println("Enter all of the currencies you want to track separated with a space (BTC ETH LINK...)");
            String[] currencyArr = sc.nextLine().toUpperCase().split(" ");
            for (String arg : currencyArr) {
                //The currency class contains all of the method calls that drive the activity of our bot
                try {
                    currencies.add(new Currency(arg, 250, true, false));
                } catch (BinanceApiException e) {
                    e.printStackTrace();
                }
            }
            long endTime = System.nanoTime();
            double time = (endTime - startTime) / 1.e9;

            System.out.println("---SETUP DONE (" + Formatter.formatDecimal(time) + " s)");

            //From this point we only use the main thread to check how the bot is doing
            while (true) {
                System.out.println("Commands: profit, active, history, wallet, currencies");
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
                            System.out.println(currency);
                        }
                        System.out.println(" ");
                        break;
                    default:
                        System.out.println("Wrong input. Try again \n");
                        break;
                }
            }
        }

    }
}
