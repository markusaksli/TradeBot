package collection;

import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceAggregatedTrades;
import com.webcerebrium.binance.datatype.BinanceSymbol;
import trading.CurrentAPI;
import trading.Formatter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PriceCollector implements Runnable {
    private final long start;
    private long end;
    public long duration;
    private final List<PriceBean> data = new ArrayList<>();
    private final BinanceSymbol symbol;
    private double lastProgress = 0;

    private static final AtomicInteger totalRequests = new AtomicInteger();
    private static final AtomicLong remaining = new AtomicLong();
    private static double progress = 0;
    private static final Semaphore minuteRequests = new Semaphore(1200);

    public List<PriceBean> getData() {
        return data;
    }

    public static void resetPermits(int minuteRequests) {
        PriceCollector.minuteRequests.drainPermits();
        PriceCollector.minuteRequests.release(minuteRequests);
    }

    public static int getRequestPermits() {
        return minuteRequests.availablePermits();
    }

    public static double getProgress() {
        return progress;
    }

    public static long getRemaining() {
        return remaining.get();
    }

    public static void setRemaining(long remaining) {
        PriceCollector.remaining.set(remaining);
    }

    public static int getTotalRequests() {
        return totalRequests.get();
    }

    public PriceCollector(long start, long end, BinanceSymbol symbol) {
        this.start = start;
        this.end = end;
        this.symbol = symbol;
        this.duration = end - start;
    }

    @Override
    public void run() {
        long startTime = end - 3600000L;
        long timeLeft = end - start;
        Map<String, Long> options = new HashMap<>();
        options.put("startTime", startTime);
        options.put("endTime", end);
        List<BinanceAggregatedTrades> trades;
        boolean isTime = false;
        while (true) {
            try {
                minuteRequests.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                trades = (CurrentAPI.get().aggTrades(symbol, 1000, options));
                if (trades.get(0).getTimestamp() == end || trades.isEmpty()) { //Empty or redundant request means we have reached the end of the chunk
                    break;
                }
            } catch (BinanceApiException e) {
                if (e.getLocalizedMessage().toLowerCase().contains("request weight")) {
                    System.out.println("---Server triggered request limit at " + Formatter.formatDate(LocalDateTime.now()) + " " + e.getLocalizedMessage());
                    minuteRequests.drainPermits();
                } else {
                    System.out.println(e.getLocalizedMessage());
                }
                continue;
            } finally {
                totalRequests.getAndIncrement();
            }

            for (int i = trades.size() - 1; i >= 0; i--) {
                BinanceAggregatedTrades trade = trades.get(i);
                if (trade.getTimestamp() < start) {
                    isTime = true;
                    break;
                }
                if (i == 0) {
                    end = trade.getTimestamp();
                }
                data.add(new PriceBean(trade.getTimestamp(), trade.getPrice().doubleValue()));
            }
            if (isTime) break;
            double currentProgress = (1 - (end - start) / (double) timeLeft);
            progress = progress - lastProgress + currentProgress;
            lastProgress = currentProgress;
            options.replace("startTime", end - 3600000L);
            options.replace("endTime", end);
        }
        remaining.getAndDecrement();
        progress = progress - lastProgress + 1;
    }
}

