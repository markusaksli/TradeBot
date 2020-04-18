package trading;

import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceAggregatedTrades;
import com.webcerebrium.binance.datatype.BinanceSymbol;

import java.text.SimpleDateFormat;
import java.util.*;

public class TradeCollector implements Runnable {
    private final Long start;
    private Long end;
    private final List<TradeBean> dataHolder;
    private final BinanceSymbol symbol;
    private double lastProgress = 0;

    private static int remaining;
    private static double progress = 0;
    private static volatile int numOfRequests = 0;

    public static double getProgress() {
        return progress;
    }

    public static int getRemaining() {
        return remaining;
    }

    public static void setRemaining(int remaining) {
        TradeCollector.remaining = remaining;
    }

    public static int getNumOfRequests() {
        return numOfRequests;
    }

    public TradeCollector(Long start, Long end, List<TradeBean> dataHolder, BinanceSymbol symbol) {
        this.start = start;
        this.end = end;
        this.dataHolder = dataHolder;
        this.symbol = symbol;
    }

    @Override
    public void run() {
        Long startTime = end - 3600000L;
        long timeLeft = end - start;
        int limit = 1000;
        Map<String, Long> options = new HashMap<>();
        options.put("startTime", startTime);
        options.put("endTime", end);
        List<BinanceAggregatedTrades> trades;
        boolean isTime = false;
        while (true) {
            while (numOfRequests > 995) {
                Thread.onSpinWait();
            }
            try {
                numOfRequests++;
                trades = (CurrentAPI.get().aggTrades(symbol, limit, options));
                BinanceAggregatedTrades trade;

                for (int i = limit - 1; i >= 0; i--) {
                    trade = trades.get(i);
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
            } catch (BinanceApiException e) {
                e.printStackTrace();
            }
        }
        remaining--;
        progress = progress - lastProgress + 1;
        System.out.println(remaining + " chunks remaining");
    }

    public static void setNumOfRequests(int numOfRequests) {
        TradeCollector.numOfRequests = numOfRequests;
    }
}
