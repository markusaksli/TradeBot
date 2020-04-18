package trading;

import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceAggregatedTrades;
import com.webcerebrium.binance.datatype.BinanceSymbol;

import java.text.SimpleDateFormat;
import java.util.*;

public class TradeCollector implements Runnable {
    private Long start;
    private Long end;
    private List<TradeBean> dataHolder;
    private BinanceSymbol symbol;
    private static int numOfRequests = 0;

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
        System.out.println("Markus");
        Long startTime = end - 3600000L;
        Long timeLeft = end - start;
        int limit = 1000;
        Map<String, Long> options = new HashMap<>();
        options.put("startTime", startTime);
        options.put("endTime", end);
        List<BinanceAggregatedTrades> trades = null;
        boolean isTime = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        while (true) {
            while (numOfRequests > 970)
                try {
                    trades = (CurrentAPI.get().aggTrades(symbol, limit, options));
                    numOfRequests++;
                    BinanceAggregatedTrades trade = null;

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
                    System.out.println(Formatter.formatPercent((end - start) / (double) timeLeft));
                    System.out.println("thread id: " + Thread.currentThread().getId());
                    options.replace("startTime", end - 3600000L);
                    options.replace("endTime", end);
                } catch (BinanceApiException e) {
                    e.printStackTrace();
                }
        }
    }

    public static void setNumOfRequests(int numOfRequests) {
        TradeCollector.numOfRequests = numOfRequests;
    }
}
