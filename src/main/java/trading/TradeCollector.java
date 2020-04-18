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
    private static volatile int numOfRequests = 0;
    private static double progress = 1;
    public static int remaining;

    public static double getProgress() {
        return progress;
    }

    public static int getRemaining() {
        return remaining;
    }

    public static int getNumOfRequests() {
        return numOfRequests;
    }

    /*BinanceSymbol symbol;
        try {
            symbol = new BinanceSymbol("BTCUSDT");
            List<TradeBean> beanList = new ArrayList<>();
            readHistory(symbol, 1587225600000L, 1587229200000L, beanList);
            Collections.reverse(beanList);
            Long chart = 1587225600000L;
            double lastPrice = 0;
            for (TradeBean tradeBean : beanList) {
                if (tradeBean.getTimestamp() >= chart) {
                    chart += 300000L;
                    System.out.println(tradeBean.getDate() + "   " + lastPrice + "   CLOSEEEEEEEEEEEEEEEEEEEEEE");
                }
                lastPrice = tradeBean.getPrice();
            }

        } catch (BinanceApiException e) {
            e.printStackTrace();
        }*/

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
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        while (true) {
            while (numOfRequests > 990) {
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
                progress = Math.min(progress, (end - start) / (double) timeLeft);
                options.replace("startTime", end - 3600000L);
                options.replace("endTime", end);
            } catch (BinanceApiException e) {
                e.printStackTrace();
            }
        }
        remaining--;
        System.out.println("Thread " + Thread.currentThread().getId() + " has finished");
    }

    public static void setNumOfRequests(int numOfRequests) {
        TradeCollector.numOfRequests = numOfRequests;
    }
}
