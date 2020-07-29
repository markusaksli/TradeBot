package modes;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.exception.BinanceApiException;
import data.PriceBean;
import data.PriceReader;
import data.PriceWriter;
import trading.CurrentAPI;
import trading.Formatter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;

public final class Collection {
    private static int chunks;
    private static String symbol;
    private static String lastMessage = "Sending requests...";
    private final static Semaphore blocker = new Semaphore(0);
    private final static Semaphore requestTracker = new Semaphore(0);
    private final static BinanceApiAsyncRestClient client = CurrentAPI.getFactory().newAsyncRestClient();
    private static boolean braked = false;
    private static boolean createdBrakeTimer = false;
    private static int brakeSeconds = 1;
    private static long initTime;

    public static void setBrakeSeconds(int brakeSeconds) {
        Collection.brakeSeconds = brakeSeconds;
    }

    public static boolean isBraked() {
        return braked;
    }

    public static void setBraked(boolean braked) {
        Collection.braked = braked;
    }

    public static Semaphore getRequestTracker() {
        return requestTracker;
    }

    public static Semaphore getBlocker() {
        return blocker;
    }

    public static BinanceApiAsyncRestClient getClient() {
        return client;
    }

    public static String getSymbol() {
        return symbol;
    }

    public static void printProgress() {
        System.out.print("\r(" + Formatter.formatDuration(System.currentTimeMillis() - initTime) + ") (" + Formatter.formatPercent((double) blocker.availablePermits() / (double) chunks) + ") " + lastMessage);
    }

    public static void setLastMessage(String lastMessage) {
        Collection.lastMessage = lastMessage;
        printProgress();
    }

    public Collection() {
        init();
    }

    private static void init() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter collectable currency (BTC, LINK, ETH...)");
        while (true) {
            try {
                symbol = sc.nextLine().toUpperCase() + "USDT";
                CurrentAPI.get().getPrice(symbol);
                break;
            } catch (BinanceApiException e) {
                System.out.println(e.getMessage());
            }
        }

        System.out.println("Enter everything in double digits. (1 = 01) \n " +
                "example: 2020/03/01 00:00:00");
        System.out.println("Date format = 'yyyy/MM/dd HH:mm:ss'");
        System.out.println("Start needs to be earlier than end\n");

        SimpleDateFormat dateFormat = Formatter.getSimpleFormatter();
        Date startDate;
        Date stopDate;
        while (true) {
            System.out.println("Enter the date you want to start from: ");
            String begin = sc.nextLine();
            System.out.println("Enter the date you want to finish with (type \"now\" for current time): ");
            String finish = sc.nextLine();
            try {
                startDate = dateFormat.parse(begin);
                if (finish.toLowerCase().equals("now")) {
                    stopDate = new Date(System.currentTimeMillis());
                } else {
                    stopDate = dateFormat.parse(finish);
                }
                if (startDate.getTime() >= stopDate.getTime() || stopDate.getTime() > System.currentTimeMillis()) {
                    System.out.println("Start needs to be earlier in time than end and end cannot be greater than current time");
                    continue;
                }
                break;
            } catch (ParseException e) {
                System.out.println(e.getLocalizedMessage());
            }
        }

        long start = startDate.getTime(); // March 1 00:00:00 1583020800000
        long end = stopDate.getTime();// April 1 00:00:00 1585699200000
        chunks = (int) Math.ceil((double) (end - start) / 3600000L);

        System.out.println("\n---Setting up...");
        String filename = Path.of("backtesting", symbol + "_" + Formatter.formatOnlyDate(start) + "-" + Formatter.formatOnlyDate(end) + ".dat").toString();
        try {
            Files.deleteIfExists(Path.of("temp"));
        } catch (IOException e) {
            System.out.println(Path.of("temp"));
            e.printStackTrace();
        }
        File tempFolder = new File("temp");
        tempFolder.mkdir();

        System.out.println("---Sending " + chunks + " requests (minimum estimate is " + (Formatter.formatDuration((long) ((double) chunks / (double) ConfigSetup.getRequestLimit() * 60000L)) + ")..."));
        int requestDelay = 60000 / ConfigSetup.getRequestLimit();
        initTime = System.currentTimeMillis();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isBraked()) {
                    requestTracker.release(1);
                } else if (!createdBrakeTimer) {
                    Timer brakeTimer = new Timer();
                    brakeTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            setLastMessage("Removing request brake");
                            setBraked(false);
                        }
                    }, brakeSeconds);
                    setLastMessage("Braked requests for " + brakeSeconds + " seconds");
                    createdBrakeTimer = true;
                }
            }
        }, requestDelay, requestDelay);
        int id = 0;
        while (true) {
            long diff = end - start;
            if (diff == 0) break;
            try {
                requestTracker.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            id++;
            client.getAggTrades(symbol, null, null, diff < 3600000L ? start : end - 3600000L, end, new TradesCallback(id, diff < 3600000L ? start : end - 3600000L, end));
            if (diff < 3600000L) break;
            end -= 3600000L;
        }
        setLastMessage("All requests sent, waiting for " + (chunks - blocker.availablePermits()) + " more requests to return");
        Collection.printProgress();
        try {
            blocker.acquire(chunks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        timer.cancel();
        System.out.print("\r(" + Formatter.formatDuration(System.currentTimeMillis() - initTime) + ") (" + Formatter.formatPercent(1.0) + ") Data collected in temp files");

        System.out.println("\n---Writing data from temp files to main file");
        try (PriceWriter writer = new PriceWriter(filename)) {
            List<Candlestick> candlesticks = CurrentAPI.get().getCandlestickBars(symbol, CandlestickInterval.FIVE_MINUTES, null, null, start);
            for (int i = 0; i < candlesticks.size() - 1; i++) {
                Candlestick candlestick = candlesticks.get(i);
                writer.writeBean(new PriceBean(candlestick.getCloseTime(), Double.parseDouble(candlestick.getClose()), true));
            }
            Candlestick lastCandle = candlesticks.get(candlesticks.size() - 1);
            long candleTime = lastCandle.getCloseTime();
            if (lastCandle.getCloseTime() == start) {
                candleTime += 300000L;
                writer.writeBean(new PriceBean(lastCandle.getCloseTime(), Double.parseDouble(lastCandle.getClose())));
            }
            PriceBean lastBean = null;
            boolean first = true;
            for (int i = chunks; i >= 1; i--) {
                System.out.print("\r(" + Formatter.formatDuration(System.currentTimeMillis() - initTime) + ") (" + Formatter.formatPercent(1 - (double) i / (double) chunks) + ") /temp/" + i + ".dat");
                File tempFile = new File("/temp/" + i + ".dat");
                try (PriceReader reader = new PriceReader(tempFile.getPath())) {
                    if (first) {
                        lastBean = reader.readPrice();
                        first = false;
                    }
                    PriceBean bean = reader.readPrice();
                    while (bean != null) {
                        if (bean.getTimestamp() > candleTime) {
                            lastBean.close();
                            while (candleTime <= bean.getTimestamp()) candleTime += 300000L;
                        }
                        writer.writeBean(lastBean);
                        lastBean = bean;
                        bean = reader.readPrice();
                    }
                    reader.close();
                    tempFile.delete();
                } catch (IOException ignored) {
                }
            }
            Files.deleteIfExists(Path.of("temp"));
            assert lastBean != null;
            writer.writeBean(lastBean);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.print("\r(" + Formatter.formatDuration(System.currentTimeMillis() - initTime) + ") (" + Formatter.formatPercent(1.0) + ") Temp files processed");

        System.out.println("\n\n---Checking data for consistency");
        long count = 0;
        boolean firstGap = true;
        boolean firstReg = true;
        try (PriceReader reader = new PriceReader(filename)) {
            PriceBean bean = reader.readPrice();
            long last = Long.MIN_VALUE;
            while (bean != null) {
                count++;
                if (bean.getTimestamp() < last) {
                    System.out.println("!-----Date regression from " + Formatter.formatDate(last) + " to " + Formatter.formatDate(bean.getTimestamp()) + "------!");
                    if (firstReg) {
                        System.out.println("!--Date regression should never occour in data. File an issue on https://github.com/markusaksli/TradeBot with your terminal history--!");
                        firstReg = false;
                    }
                }
                if (bean.getTimestamp() - last > 1800000L && !bean.isClosing()) {
                    if (firstGap) {
                        System.out.println("-Gaps (checking for 30min+) usually point to exchange maintenance times, check https://www.binance.com/en/trade/pro/" + symbol.replace("USDT", "") + "_USDT if suspicious");
                        firstGap = false;
                    }
                    System.out.println("Gap from " + Formatter.formatDate(last) + " to " + Formatter.formatDate(bean.getTimestamp()));
                }
                last = bean.getTimestamp();
                bean = reader.readPrice();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print(firstGap && firstReg ? "---Data is completely consistent" : "");

        System.out.println("\n---Collection completed in "
                + Formatter.formatDuration(System.currentTimeMillis() - initTime) + ", result in "
                + filename
                + " (" + Formatter.formatLarge(count) + " entries, " + Formatter.formatDecimal((double) new File(filename).length() / 1048576.0) + " MB)");
        System.out.println("---Files may only appear after quitting");

        while (true) {
            System.out.println("Type \"quit\" to quit, type \"result\" to create text file with price data");
            String s = sc.nextLine();
            if (s.toLowerCase().equals("quit")) {
                System.exit(0);
                break;
            } else if (s.toLowerCase().equals("result")) {
                System.out.println("Writing...");
                try (PriceReader reader = new PriceReader(filename); PrintWriter writer = new PrintWriter("result.txt")) {
                    PriceBean bean = reader.readPrice();
                    while (bean != null) {
                        writer.write(bean.toString() + "\n");
                        bean = reader.readPrice();
                    }
                    System.out.println("Result of collection written to result.txt");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.exit(0);
    }
}

class TradesCallback implements BinanceApiCallback<List<AggTrade>> {
    int id;
    long start;
    long end;

    public TradesCallback(int id, long start, long end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    @Override
    public void onFailure(Throwable cause) {
        try {
            Collection.setLastMessage("Request " + id + " failed due to: \"" + cause.getMessage() + "\"");
            if (cause.getMessage().toLowerCase().contains("weight")) {
                Collection.setBrakeSeconds(cause.getMessage().toLowerCase().contains("banned") ? 60 : 1);
                Collection.setBraked(true);
            }
            Collection.getRequestTracker().acquire();
            Collection.getClient().getAggTrades(Collection.getSymbol(), null, null, start, end, new TradesCallback(id, start, end));
            Collection.setLastMessage("Resent request " + id);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResponse(List<AggTrade> response) {
        if (!response.isEmpty()) {
            try (PriceWriter writer = new PriceWriter("/temp/" + id + ".dat")) {
                double lastPrice = Double.parseDouble(response.get(0).getPrice());
                for (int i = 1; i < response.size(); i++) {
                    AggTrade trade = response.get(i);
                    double newPrice = Double.parseDouble(trade.getPrice());
                    if (lastPrice == newPrice) continue;
                    lastPrice = newPrice;
                    writer.writeBean(new PriceBean(trade.getTradeTime(), newPrice));
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        Collection.getBlocker().release();
        Collection.printProgress();
    }
}
