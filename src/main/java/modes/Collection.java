package modes;

import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import data.PriceBean;
import data.PriceReader;
import data.PriceWriter;
import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.exception.BinanceApiException;
import trading.CurrentAPI;
import trading.Formatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Semaphore;

//TODO: Check Collection logic
public final class Collection {
    private static int chunks;
    private static String symbol;
    private final static Semaphore blocker = new Semaphore(0);
    private final static BinanceApiAsyncRestClient client = CurrentAPI.getFactory().newAsyncRestClient();

    public static Semaphore getBlocker() {
        return blocker;
    }

    public static BinanceApiAsyncRestClient getClient() {
        return client;
    }

    public static String getSymbol() {
        return symbol;
    }

    public static double getProgress() {
        return (double) blocker.availablePermits() / (double) chunks;
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
        System.out.println("Date format = 'yyyy/MM/dd HH:mm:ss' \n");
        System.out.println("Start needs to be earlier than end");

        SimpleDateFormat dateFormat = Formatter.getSimpleFormatter();
        Date startDate;
        Date stopDate;
        while (true) {
            System.out.println("Enter the date you want to start from: ");
            String begin = sc.nextLine();
            System.out.println("Enter the date you want to finish with (type \"now\" for current time): ");
            String finish = sc.nextLine();
            try {
                if (Formatter.isValidDateFormat("yyyy/MM/dd HH:mm:ss", begin) &&
                        Formatter.isValidDateFormat("yyyy/MM/dd HH:mm:ss", finish)) {
                    startDate = dateFormat.parse(begin);
                    if (finish.equals("now")) {
                        stopDate = Date.from(Instant.now());
                    } else {
                        stopDate = dateFormat.parse(finish);
                    }
                    if (startDate.getTime() >= stopDate.getTime() || stopDate.getTime() > CurrentAPI.get().getServerTime()) {
                        System.out.println("Start needs to be earlier in time than end and end cannot be greater than current server time");
                        continue;
                    }
                    break;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        long start = startDate.getTime(); // March 1 00:00:00 1583020800000
        long end = stopDate.getTime();// April 1 00:00:00 1585699200000
        chunks = (int) Math.ceil((double) (end - start) / 3600000L);

        System.out.println("---Setting up...");
        String filename = Path.of("backtesting", symbol + "_" + Formatter.formatOnlyDate(start) + "-" + Formatter.formatOnlyDate(end) + ".dat").toString();
        try {
            Files.deleteIfExists(Path.of("temp"));
        } catch (IOException e) {
            System.out.println(Path.of("temp"));
            e.printStackTrace();
        }
        File tempFolder = new File("temp");
        tempFolder.mkdir();

        System.out.println("---Sending requests (1200 per minute)...");
        Semaphore requestTracker = new Semaphore(1199);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                requestTracker.release(1199);
            }
        }, 61 * 1000, 61 * 1000);
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
        timer.cancel();
        System.out.println("\n---All request submitted");
        System.out.println("---Waiting for " + (chunks - blocker.availablePermits()) + " more requests to return");
        System.out.print(Collection.getProgress());
        try {
            blocker.acquire(chunks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.print("\r" + Formatter.formatPercent(1.0));

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
                System.out.print("\r" + Formatter.formatPercent(1 - (double) i / (double) chunks));
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
                } catch (IOException e) {
                    continue;
                }
                tempFile.delete();
            }
            assert lastBean != null;
            writer.writeBean(lastBean);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.deleteIfExists(Path.of("temp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.print("\r" + Formatter.formatPercent(1.0));

        System.out.println("\n---Checking data for consistency");
        try (PriceReader reader = new PriceReader(filename); PrintWriter writer = new PrintWriter("result.txt")) {
            PriceBean bean = reader.readPrice();
            long last = Long.MIN_VALUE;
            while (bean != null) {
                writer.write(bean + "\n");
                if (bean.getTimestamp() < last) {
                    System.out.println("---Date regression in data!");
                    System.out.println(Formatter.formatDate(last) + "   " + Formatter.formatDate(bean.getTimestamp()));
                }
                if (bean.getTimestamp() - last > 60000L && !bean.isClosing())
                    System.out.println("Gap from " + Formatter.formatDate(last) + " to " + Formatter.formatDate(bean.getTimestamp()));
                last = bean.getTimestamp();
                bean = reader.readPrice();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("---Collection completed, result in "
                + filename
                + " (" + Formatter.formatDecimal((double) new File(filename).length() / 1048576.0) + " MB)");

        while (true) {
            System.out.println("Type \"quit\" to quit");
            String s = sc.nextLine();
            if (s.toLowerCase().equals("quit")) {
                System.exit(0);
                break;
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
            Collection.getBlocker().acquire();
            Collection.getClient().getAggTrades(Collection.getSymbol(), null, null, start, end, this);
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
        System.out.print("\r" + Formatter.formatPercent(Collection.getProgress()));
    }
}
