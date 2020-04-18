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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Main {
    static Set<Currency> currencies; //There should never be two of the same Currency

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("---Enter bot mode (live, simulated, backtesting, collection)");
            String mode = sc.nextLine();
            switch (mode) {
                case "live":
                    Mode.set(Mode.LIVE);
                    break;
                case "simulated":
                    Mode.set(Mode.SIMULATED);
                    break;
                case "backtesting":
                    Mode.set(Mode.BACKTESTING);
                    break;
                case "collection":
                    Mode.set(Mode.COLLECTION);
                    break;
                default:
                    System.out.println("Incorrect mode");
                    continue;
            }
            break;
        }
        System.out.println("---Entering " + Mode.get().name() + " mode");


        if (Mode.get() == Mode.COLLECTION) {
            System.out.println("Enter desired currency pair");
            BinanceSymbol symbol = null;
            try {
                symbol = new BinanceSymbol(sc.nextLine());
            } catch (BinanceApiException e) {
                e.printStackTrace();
            }
            System.out.println("Enter start of collection period (Unix epoch milliseconds)");
            long start = sc.nextLong(); // 1. märts hour 0
            System.out.println("Enter end of collection period (Unix epoch milliseconds)");
            long end = sc.nextLong(); // 1. märts hour 0

            List<TradeBean> dataHolder = new ArrayList<>();
            long wholePeriod = end - start;
            int numOfThreads = 30;
            long toSubtract = wholePeriod / (long) numOfThreads;

            TradeCollector.setRemaining(numOfThreads);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            TradeBean.setDateFormat(dateFormat);

            final ExecutorService executorService = Executors.newCachedThreadPool();
            List<Future<?>> futures = new ArrayList<>();
            Instant initTime = Instant.now();
            for (int i = 0; i < numOfThreads - 1; i++) {
                futures.add(executorService.submit(new TradeCollector(end - toSubtract, end, dataHolder, symbol)));
                end -= toSubtract;
            }
            executorService.submit(new TradeCollector(start, end, dataHolder, symbol));
            System.out.println("---Started collection.");

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
                        System.out.println("---Overall progress : " + Formatter.formatPercent(TradeCollector.getProgress() / numOfThreads) + ", remaining chunks: " + TradeCollector.getRemaining() + "/" + numOfThreads + ", number of requests hit in last minute: " + TradeCollector.getNumOfRequests());
                        timeElapsed = false;
                        TradeCollector.setNumOfRequests(0);
                    }
                }
            }
            executorService.shutdown();
            try {
                executorService.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while (true) {
                try {
                    dataHolder.sort(Comparator.comparing(TradeBean::getTimestamp));
                    break;
                } catch (NullPointerException e) {
                    System.out.println("Could not find list to sort");
                }
            }
            System.out.println("---Collected: " + dataHolder.size()
                    + " aggregated trades from " + dataHolder.get(0).getDate()
                    + " to " + dataHolder.get(dataHolder.size() - 1).getDate()
                    + " in " + Formatter.formatDuration(Duration.between(initTime, Instant.now())));

            String filename = "backtesting\\" + symbol + "_" + start + "_" + end + ".txt";
            try (FileWriter writer = new FileWriter(filename)) {
                System.out.println("---Writing file");
                writer.write(dataHolder.size() + " aggregated trades from " + dataHolder.get(0).getDate() + " to " + dataHolder.get(dataHolder.size() - 1).getDate() + "\n");
                start += 300000;
                for (int i = 0; i < dataHolder.size(); i++) {
                    writer.write(dataHolder.get(i).toString() + "\n");
                    if (i < dataHolder.size() - 3) {
                        if (dataHolder.get(i + 2).getTimestamp() > start) {
                            dataHolder.get(i + 1).close();
                            start += 300000;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("---Collection completed, result in "
                    + filename
                    + " (" + Formatter.formatDecimal((double) new File(filename).length() / (double) 1024 / (double) 1024) + " MB)");
        } else if (Mode.get() == Mode.BACKTESTING) {

        } else {
            Account toomas = new Account("Investor Toomas", 1000);
            BuySell.setAccount(toomas);

            /*while (true) {
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
            }*/

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
            for (String arg : args) {
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

        //From this point we only use the main thread to check how the bot is doing
    }
}
