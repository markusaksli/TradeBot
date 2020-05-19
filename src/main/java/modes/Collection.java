package modes;

import collection.PriceBean;
import collection.PriceCollector;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import trading.Formatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Collection {
    private static long minutesForCollection;

    public Collection() {
        init();
    }

    public static void setMinutesForCollection(long minutesForCollection) {
        Collection.minutesForCollection = minutesForCollection;
    }

    private static void init() {
        Scanner sc = new Scanner(System.in);
        SimpleDateFormat dateFormat = Formatter.getSimpleFormatter();
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        System.out.println("Enter collectable currency (BTC, LINK, ETH...)");
        BinanceSymbol symbol = null;
        try {
            symbol = new BinanceSymbol(sc.nextLine().toUpperCase() + "USDT");
        } catch (BinanceApiException e) {
            e.printStackTrace();
        }

        System.out.println("Enter everything in double digits. (1 = 01) \n " +
                "example: 2020/03/01 00:00:00");
        System.out.println("Date format = 'yyyy/MM/dd HH:mm:ss' \n");


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
                    break;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        long start = startDate.getTime(); // March 1 00:00:00 1583020800000
        long end = stopDate.getTime();// April 1 00:00:00 1585699200000

        System.out.println("---Setting up...");
        String filename = Path.of("backtesting", symbol + "_" + Formatter.formatOnlyDate(start) + "-" + Formatter.formatOnlyDate(end) + ".txt").toString();
        long wholePeriod = end - start;
        long toSubtract = minutesForCollection * 60000;
        long chunks = wholePeriod / toSubtract;//Optimal number to reach 1200 requests per min is about 30

        PriceCollector.setRemaining(chunks);
        PriceBean.setDateFormat(dateFormat);

        final ExecutorService executorService = Executors.newCachedThreadPool();
        List<PriceCollector> collectors = new ArrayList<>();

        Instant initTime = Instant.now();
        long minuteEpoch = initTime.toEpochMilli();
        for (int i = 0; i < chunks - 1; i++) {
            PriceCollector collector = new PriceCollector(end - toSubtract, end, symbol);
            collectors.add(collector);
            executorService.submit(collector);
            end -= toSubtract;
        }
        while (System.currentTimeMillis() > minuteEpoch) minuteEpoch += 60000L;
        PriceCollector finalCollector = new PriceCollector(start, end, symbol); //Final chunk is right up to start in case of uneven division
        collectors.add(finalCollector);
        executorService.submit(finalCollector);
        System.out.println("---Finished creating " + collectors.size() + " chunk collectors");

        while (collectors.stream().anyMatch(collector -> !collector.isDone())) {
            if (System.currentTimeMillis() > minuteEpoch) {
                System.out.println("---"
                        + Formatter.formatDate(LocalDateTime.now())
                        + " Progress: " + Formatter.formatPercent(PriceCollector.getProgress() / chunks)
                        + ", chunks: " + (chunks - PriceCollector.getRemaining()) + "/" + chunks
                        + ", total requests: " + PriceCollector.getTotalRequests());
                if (PriceCollector.getRequestPermits() > 0) {
                    System.out.println("------Bot has not used " + PriceCollector.getRequestPermits() + "/1200 requests");
                }
                minuteEpoch += 60000L;
                PriceCollector.resetPermits(1200);
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
                        while (finalData.get(i + 2).getTimestamp() > start) start += 300000;
                        finalData.get(i + 1).close();
                    }
                }
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
            } else {
                System.out.println("Wrong input. ");
            }
        }
    }
}
