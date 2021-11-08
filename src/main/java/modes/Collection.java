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
import org.apache.commons.io.FileUtils;
import system.ConfigSetup;
import trading.CurrentAPI;
import system.Formatter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//TODO: Identify cause and fix occasional date regression (maybe related to resending?)
public final class Collection {
    private static String symbol;
    private static String lastMessage = "Sending requests...";
    private static AtomicBoolean braked = new AtomicBoolean(false);
    private static boolean createdBrakeTimer = false;
    private static AtomicInteger brakeSeconds = new AtomicInteger(2);
    private static int chunks;
    private static long initTime;
    private static final Scanner sc = new Scanner(System.in);
    private static final File backtestingFolder = new File("backtesting");

    public static final String INTERRUPT_MESSAGE = "Thread interrupted while waiting for request permission";
    private static final Semaphore downloadCompletionBlocker = new Semaphore(0);
    private static final Semaphore requestTracker = new Semaphore(0);
    private static final BinanceApiAsyncRestClient client = CurrentAPI.getFactory().newAsyncRestClient();

    private Collection() {
        throw new IllegalStateException("Utility class");
    }

    public static void setBrakeSeconds(int brakeSeconds) {
        Collection.brakeSeconds.set(brakeSeconds);
    }

    public static boolean isBraked() {
        return Collection.braked.get();
    }

    public static void setBraked(boolean braked) {
        if (braked) {
            requestTracker.drainPermits();
        }
        Collection.braked.set(braked);
    }

    public static Semaphore getRequestTracker() {
        return requestTracker;
    }

    public static void downloadBlockerRelease() {
        downloadCompletionBlocker.release();
    }

    public static BinanceApiAsyncRestClient getClient() {
        return client;
    }

    public static String getSymbol() {
        return symbol;
    }

    public static void printProgress() {
        double progress = (double) downloadCompletionBlocker.availablePermits() / (double) chunks;
        long time = System.currentTimeMillis() - initTime;
        System.out.print("\r"
                + Formatter.formatDate(System.currentTimeMillis())
                + " ("
                + Formatter.formatPercent(progress)
                + ") ("
                + Formatter.formatDuration((long) Math.ceil((time / progress) - time))
                + ") " + lastMessage);
    }

    public static void setLastMessage(String lastMessage) {
        Collection.lastMessage = lastMessage.replaceAll("\n", "   ");
        printProgress();
    }

    private static boolean collectionInterface() {
        if (backtestingFolder.exists() && backtestingFolder.isDirectory()) {
            String[] backtestingFiles = getDataFiles();
            if (backtestingFiles.length == 0) {
                System.out.println("---No backtesting files detected");
                return true;
            }

            String input = "";
            while (!input.equalsIgnoreCase("new")) {
                if (input.equalsIgnoreCase("quit")) {
                    System.exit(0);
                } else if (input.equalsIgnoreCase("modes")) {
                    return false;
                }
                if (input.matches("\\d+")) {
                    int index = Integer.parseInt(input);
                    if (index <= backtestingFiles.length) {
                        describe("backtesting/" + backtestingFiles[index - 1]);
                    }
                }
                System.out.println("\nEnter a number to describe the backtesting data file\n");
                for (int i = 0; i < backtestingFiles.length; i++) {
                    System.out.println("[" + (i + 1) + "] " + backtestingFiles[i]);
                }
                System.out.println("\nEnter \"new\" to start collecting a new data file");
                System.out.println("Enter \"quit\" to exit the program");
                System.out.println("Enter \"modes\" to return to mode selection.\n");
                input = sc.nextLine();
            }
        } else {
            System.out.println("---No backtesting files detected");
        }
        return true;
    }

    public static String[] getDataFiles() {
        String[] backtestingFiles = backtestingFolder.list();
        if (backtestingFiles == null) {
            return new String[0];
        }
        return backtestingFiles;
    }

    public static void startCollection() {
        File tempFolder = new File("temp");
        if (tempFolder.exists() && tempFolder.isDirectory()) {
            if (Objects.requireNonNull(tempFolder.list()).length > 0 && new File("temp/recovery").exists()) {
                System.out.println("---Temp backtesting files detected");
                System.out.println("Type \"compile\" to attempt to compile the downloaded data or \"delete\" to delete them");
                String input = sc.nextLine();
                if (input.equalsIgnoreCase("delete")) {
                    deleteTemp();
                }
                if (input.equalsIgnoreCase("compile")) {
                    Long start = null;
                    String filename = null;
                    try {
                        final List<String> lines = Files.readAllLines(Path.of("temp/recovery"));
                        filename = lines.get(0);
                        start = Long.parseLong(lines.get(1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (start != null && filename != null) {
                        try {
                            compileBackTestingData(start, filename);

                            checkBacktestingData(filename);

                            System.out.println("\n---Collection completed, result in "
                                    + new File(filename).getAbsolutePath());

                            describe(filename);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("---Recovery failed, removing temp files");
                            deleteTemp();
                            try {
                                Files.deleteIfExists(Path.of(filename));
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    }
                }
            } else {
                deleteTemp();
            }
        }
        boolean returnToModes = collectionInterface();
        if (!returnToModes) {
            return;
        }
        System.out.println("Enter collectable currency (BTC, LINK, ETH...)");
        while (true) {
            try {
                symbol = sc.nextLine().toUpperCase() + ConfigSetup.getFiat();
                CurrentAPI.get().getPrice(symbol);
                break;
            } catch (BinanceApiException e) {
                System.out.println("Got error for symbol " + symbol + ": " + e.getMessage());
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
                startDate = dateFormat.parse(begin.replace("mn", "00:00:00"));
                if (finish.equalsIgnoreCase("now")) {
                    stopDate = new Date(System.currentTimeMillis());
                } else {
                    stopDate = dateFormat.parse(finish.replace("mn", "00:00:00"));
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

        deleteTemp();
        new File("temp").mkdir();
        if (!(backtestingFolder.exists() && backtestingFolder.isDirectory())) backtestingFolder.mkdir();

        try (final FileWriter fos = new FileWriter("temp/recovery")) {
            fos.write(filename + "\n" + start);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int requestDelay = 60000 / ConfigSetup.getRequestLimit();
        System.out.println("---Request delay: " + requestDelay + " ms (" + ConfigSetup.getRequestLimit() + " per minute)");
        System.out.println("---Sending " + chunks + " requests (minimum estimate is " + (Formatter.formatDuration((long) ((double) chunks / (double) ConfigSetup.getRequestLimit() * 60000L)) + ")..."));
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
                            createdBrakeTimer = false;
                        }
                    }, brakeSeconds.get());
                    System.out.println("\n");
                    setLastMessage("Braked requests for " + brakeSeconds + " seconds");
                    createdBrakeTimer = true;
                }
            }
        }, requestDelay, requestDelay);

        Collection.setLastMessage("Sending requests...");
        int id = 0;
        while (true) {
            long diff = end - start;
            if (diff == 0) break;
            try {
                requestTracker.acquire();
            } catch (InterruptedException e) {
                setLastMessage(INTERRUPT_MESSAGE);
                Thread.currentThread().interrupt();
            }
            id++;
            long requestStart = diff < 3600000L ? start : end - 3600000L;
            client.getAggTrades(symbol, null, null, requestStart, end, new TradesCallback(id, requestStart, end, 0));
            if (diff < 3600000L) break;
            end -= 3600000L;
        }
        try {
            downloadCompletionBlocker.acquire(chunks);
        } catch (InterruptedException e) {
            setLastMessage(INTERRUPT_MESSAGE);
            Thread.currentThread().interrupt();
        }
        timer.cancel();
        timer.purge();
        System.out.print("\r(" + Formatter.formatDuration(System.currentTimeMillis() - initTime) + ") (" + Formatter.formatPercent(1.0) + ") Data collected in temp files");

        compileBackTestingData(start, filename);

        checkBacktestingData(filename);

        System.out.println("\n---Collection completed in "
                + Formatter.formatDuration(System.currentTimeMillis() - initTime) + ", result in "
                + new File(filename).getAbsolutePath());

        describe(filename);

        startCollection();
    }

    public static void dataToCsv(String filename) {
        System.out.println("Writing .csv file...");
        final String csv = filename.replace(".dat", ".csv").replace("backtesting", "csv");
        new File("csv").mkdir();
        try (PriceReader reader = new PriceReader(filename); PrintWriter writer = new PrintWriter(csv)) {
            writer.write("timestamp,price,is5minClosing\n");
            PriceBean bean = reader.readPrice();
            while (bean != null) {
                writer.write(bean.toCsvString() + "\n");
                bean = reader.readPrice();
            }
            System.out.println("Result of collection written to " + new File(csv).getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean compileBackTestingData(long start, String filename) {
        System.out.println("\n---Writing data from temp files to main file");
        try {
            Files.deleteIfExists(Path.of(filename));
        } catch (IOException e) {
            System.out.println("---Could not automatically delete previous file at " + filename);
            return false;
        }
        symbol = filename.split("[/\\\\]")[1].split("_")[0];
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
                File tempFile = new File("temp/" + i + ".dat");
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
                } catch (FileNotFoundException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
                deleteTempFile(tempFile);
            }
            assert lastBean != null;
            writer.writeBean(lastBean);
        } catch (IOException e) {
            System.out.println();
            e.printStackTrace();
            System.out.println("\n---Could not compile backtesting data into main file from temp files!");
            return false;
        }
        deleteTemp();
        System.out.print("\r(" + Formatter.formatDuration(System.currentTimeMillis() - initTime) + ") (" + Formatter.formatPercent(1.0) + ") Temp files processed");
        return true;
    }

    private static void deleteTempFile(File tempFile) {
        try {
            Files.delete(tempFile.toPath());
        } catch (NoSuchFileException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkBacktestingData(String filename) {
        if (!Files.exists(Path.of(filename))) {
            System.out.println("\n---File at " + filename + " does not exist!");
            return;
        }
        System.out.println("\n\n---Checking data for consistency");
        boolean firstGap = true;
        boolean firstReg = true;
        try (PriceReader reader = new PriceReader(filename)) {
            PriceBean bean = reader.readPrice();
            long last = Long.MIN_VALUE;
            while (bean != null) {
                if (bean.getTimestamp() < last) {
                    System.out.println("!-----Date regression from " + Formatter.formatDate(last) + " to " + Formatter.formatDate(bean.getTimestamp()) + "------!");
                    if (firstReg) {
                        System.out.println("!--Date regression should never occour in data. File an issue on https://github.com/markusaksli/TradeBot with your terminal history--!");
                        firstReg = false;
                    }
                }
                if (bean.getTimestamp() - last > 1800000L && !bean.isClosing()) {
                    if (firstGap) {
                        System.out.println("-Gaps (checking for 30min+) usually point to exchange maintenance times, check https://www.binance.com/en/trade/pro/" + symbol.replace(ConfigSetup.getFiat(), "_" + ConfigSetup.getFiat()) + " if suspicious");
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
        return;
    }

    private static void deleteTemp() {
        try {
            FileUtils.deleteDirectory(new File("temp"));
        } catch (IOException e) {
            System.out.println("---Could not automatically delete temp folder!");
        }
    }

    public static void describe(String filename) {
        try (PriceReader reader = new PriceReader(filename)) {
            long count = 0;
            long totalTimeDiff = 0;
            long max = Integer.MIN_VALUE;
            PriceBean bean = reader.readPrice();
            long lastTime = bean.getTimestamp();
            bean = reader.readPrice();
            while (bean.getTimestamp() - lastTime > 290000L) {
                lastTime = bean.getTimestamp();
                bean = reader.readPrice();
            }
            while (bean != null) {
                final long timeDiff = bean.getTimestamp() - lastTime;
                if (timeDiff >= 300000L) {
                    lastTime = bean.getTimestamp();
                    bean = reader.readPrice();
                    continue;
                }
                count++;
                totalTimeDiff += timeDiff;
                if (timeDiff > max) {
                    max = timeDiff;
                }
                lastTime = bean.getTimestamp();
                bean = reader.readPrice();
            }
            System.out.println("---File contains: " + Formatter.formatLarge(count) + " entries (average interval " + Formatter.formatDecimal((double) totalTimeDiff / count) + " ms)");
            System.out.println("-Longest gap in consistent data: " + Formatter.formatDuration(max));
            System.out.println("---Covered time period: " + Formatter.formatDuration(totalTimeDiff));
            System.out.println("---File size: " + Formatter.formatDecimal((double) new File(filename).length() / 1048576.0) + " MB");

            while (true) {
                System.out.println("\nEnter \"back\" to return, \"check\" to verify the data, and \"csv\" to create .csv file with price data");
                String s = sc.nextLine();
                //TODO: Method to get csv with indicators for ML (5min, interval, realtime)
                //https://github.com/markrkalder/crypto-ds/blob/transformer/src/main/java/ml/DataCalculator.java
                if (s.equalsIgnoreCase("back")) {
                    return;
                } else if (s.equalsIgnoreCase("csv")) {
                    dataToCsv(filename);
                } else if (s.equalsIgnoreCase("check")) {
                    checkBacktestingData(filename);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class TradesCallback implements BinanceApiCallback<List<AggTrade>> {
    int id;
    long start;
    long end;
    int retry;

    public TradesCallback(int id, long start, long end, int retry) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.retry = retry;
    }

    @Override
    public void onFailure(Throwable cause) {
        try {
            int validRetry = 0;
            if (cause.getMessage().toLowerCase().contains("weight")) {
                if (cause.getMessage().toLowerCase().contains("banned")) {
                    long bannedTime = Long.parseLong(cause.getMessage().split("until ")[1].split("\\.")[0]);
                    int waitTime = Math.toIntExact((bannedTime - System.currentTimeMillis()) / 1000) + 1;
                    Collection.setLastMessage("Banned by Binance API until " + Formatter.formatDate(bannedTime) + " (" + Formatter.formatDuration(waitTime * 1000L) + ")");
                    Collection.setBrakeSeconds(waitTime);
                } else {
                    Collection.setLastMessage("Got warned for sending too many requests");
                    Collection.setBrakeSeconds(2);
                }
                Collection.setBraked(true);
            } else {
                validRetry = 1;
                if (retry == 10) {
                    Collection.downloadBlockerRelease();
                    Collection.setLastMessage("Request " + id + " failed after 10 retry attempts due to " + cause.getMessage());
                    return;
                }
                Collection.setLastMessage("Request " + id + " failed due to: \"" + cause.getMessage() + "\"");
            }
            Collection.getRequestTracker().acquire();
            Collection.getClient().getAggTrades(Collection.getSymbol(), null, null, start, end, new TradesCallback(id, start, end, retry + validRetry));
            Collection.setLastMessage("Resent request " + id);
        } catch (InterruptedException e) {
            Collection.setLastMessage(Collection.INTERRUPT_MESSAGE);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onResponse(List<AggTrade> response) {
        if (!response.isEmpty()) {
            try (PriceWriter writer = new PriceWriter("temp/" + id + ".dat")) {
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
        Collection.downloadBlockerRelease();
        Collection.printProgress();
    }
}
