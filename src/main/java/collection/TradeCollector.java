package collection;

import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceAggregatedTrades;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import trading.CurrentAPI;
import trading.Formatter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TradeCollector implements Runnable {
    private final Long start;
    private long end;
    public long duration;
    private final List<TradeBean> dataHolder;
    private final BinanceSymbol symbol;
    private double lastProgress = 0;

    private static int threads;
    private static int totalRequests;
    private static long remaining;
    private static double progress = 0;
    private static final AtomicInteger minuteRequests = new AtomicInteger();

    public static void setMinuteRequests(int minuteRequests) {
        TradeCollector.minuteRequests.set(minuteRequests);
    }

    public static int getThreads() {
        return threads;
    }

    public static double getProgress() {
        return progress;
    }

    public static long getRemaining() {
        return remaining;
    }

    public static void setRemaining(long remaining) {
        TradeCollector.remaining = remaining;
    }

    public static int getTotalRequests() {
        return totalRequests;
    }

    public TradeCollector(long start, long end, List<TradeBean> dataHolder, BinanceSymbol symbol) {
        this.start = start;
        this.end = end;
        this.dataHolder = dataHolder;
        this.symbol = symbol;
        this.duration = end - start;
    }

    @Override
    public void run() {
        threads++;
        Long startTime = end - 3600000L;
        long timeLeft = end - start;
        int limit = 1000;
        Map<String, Long> options = new HashMap<>();
        options.put("startTime", startTime);
        options.put("endTime", end);
        List<BinanceAggregatedTrades> trades = null;
        boolean isTime = false;
        while (true) {
            while (minuteRequests.get() >= 1170) {
                //TODO: This still sleeps the CPU to 100% usage and causes temporary performance issues, needs optimisation.
                Thread.onSpinWait();
            }

            minuteRequests.getAndIncrement();

            if (minuteRequests.get() == 1170) {
                System.out.println("---Request limit per minute hit at " + Formatter.formatDate(LocalDateTime.now()));
            }

            try {
                trades = (CurrentAPI.get().aggTrades(symbol, limit, options));
                totalRequests++;
            } catch (BinanceApiException e) {
                System.out.println("---Server triggered request limit at "
                        + Formatter.formatDate(LocalDateTime.now())
                        + (e.getLocalizedMessage().toLowerCase().contains("banned") ? "   " + e.getLocalizedMessage() : ""));
                minuteRequests.set(1200);
            }

            for (int i = limit - 1; i >= 0; i--) {
                assert trades != null;
                BinanceAggregatedTrades trade = trades.get(i);
                if (trade.getTimestamp() < start) {
                    isTime = true;
                    break;
                }
                if (i == 0) {
                    end = trade.getTimestamp();
                }

                dataHolder.add(new TradeBean(trade.getPrice().doubleValue(), trade.getTimestamp()));
            }
            if (isTime) break;
            double currentProgress = (1 - (end - start) / (double) timeLeft);
            progress = progress - lastProgress + currentProgress;
            lastProgress = currentProgress;
            options.replace("startTime", end - 3600000L);
            options.replace("endTime", end);
        }
        remaining--;
        progress = progress - lastProgress + 1;
        threads--;
    }
}

