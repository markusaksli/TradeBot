package modes;

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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;

//TODO: Check Collection logic
public final class Collection {
    private static long minutesForCollection;
    private final static Semaphore blocker = new Semaphore(0);

    public static void addPermit() {
        blocker.release();
    }

    public static int getPermitCount() {
        return blocker.availablePermits();
    }

    public Collection() {
        init();
    }

    public static void setMinutesForCollection(long minutesForCollection) {
        Collection.minutesForCollection = minutesForCollection;
    }

    private static void init() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter collectable currency (BTC, LINK, ETH...)");
        String symbol;
        while (true) {
            try {
                symbol = sc.nextLine().toUpperCase() + "USDT";
                CurrentAPI.get().getPrice(symbol);
                break;
            } catch (BinanceApiException e) {
                System.out.println("Invalid symbol, try again");
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
            System.out.println("Enter the date you want to finish with: ");
            String finish = sc.nextLine();
            try {
                if (Formatter.isValidDateFormat("yyyy/MM/dd HH:mm:ss", begin) &&
                        Formatter.isValidDateFormat("yyyy/MM/dd HH:mm:ss", finish)) {
                    startDate = dateFormat.parse(begin);
                    stopDate = dateFormat.parse(finish);
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

        System.out.println("Server: " + Formatter.formatDate(CurrentAPI.get().getServerTime()) + " Collection end: " + Formatter.formatDate(end));

        int chunks = 0;
        BinanceApiAsyncRestClient client = CurrentAPI.getFactory().newAsyncRestClient();
        while (true) {
            long diff = end - start;
            if (diff == 0) break;
            chunks++;
            client.getAggTrades(symbol, null, null, diff < 3600000L ? start : end - 3600000L, end, new TradesCallback(chunks));
            if (diff < 3600000L) break;
            end -= 3600000L;
        }
        System.out.println("Waiting for " + chunks + " requests to finish");

        try {
            blocker.acquire(chunks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //TODO: Add isClose tagging and predata for indicators
        try (PriceWriter writer = new PriceWriter(filename)) {
            for (int i = chunks; i >= 1; i--) {
                File tempFile = new File("/temp/" + i + ".dat");
                try (PriceReader reader = new PriceReader(tempFile.getPath())) {
                    PriceBean bean = reader.readPrice();
                    while (bean != null) {
                        writer.writeBean(bean);
                        bean = reader.readPrice();
                    }
                }
                tempFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.deleteIfExists(Path.of("temp"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try (PriceReader reader = new PriceReader(filename)) {
            PriceBean bean = reader.readPrice();
            long last = Long.MIN_VALUE;
            while (bean != null) {
                if (bean.getTimestamp() < last) {
                    System.out.println("Date regression in data!");
                    System.out.println(Formatter.formatDate(last) + "   " + Formatter.formatDate(bean.getTimestamp()));
                }
                last = bean.getTimestamp();
                bean = reader.readPrice();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}

class TradesCallback implements BinanceApiCallback<List<AggTrade>> {
    int id;

    public TradesCallback(int id) {
        this.id = id;
    }

    @Override
    public void onResponse(List<AggTrade> response) {
        try (PriceWriter writer = new PriceWriter("/temp/" + id + ".dat")) {
            double lastPrice = Double.parseDouble(response.get(0).getPrice());
            for (int i = 1; i < response.size(); i++) {
                AggTrade trade = response.get(i);
                double newPrice = Double.parseDouble(trade.getPrice());
                if (lastPrice == newPrice) continue;
                writer.writeBean(new PriceBean(trade.getTradeTime(), newPrice));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Collection.addPermit();
        System.out.println("Request " + id + " has finished, number of permits: " + Collection.getPermitCount());
    }
}
