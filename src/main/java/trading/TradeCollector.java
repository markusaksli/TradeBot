package trading;

import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceAggregatedTrades;
import com.webcerebrium.binance.datatype.BinanceCandlestick;
import com.webcerebrium.binance.datatype.BinanceInterval;
import com.webcerebrium.binance.datatype.BinanceSymbol;

import java.text.SimpleDateFormat;
import java.util.*;

public class TradeCollector {

    public static void main(String[] args) {
        BinanceSymbol symbol = null;
        try {
            symbol = new BinanceSymbol("BTCUSDT");
        } catch (BinanceApiException e) {
            e.printStackTrace();
        }
        List<TradeBean> beanList = new ArrayList<>();
        readHistory(symbol, 1583013600000L, 1583017200000L, beanList);
    }

    public static void readHistory(BinanceSymbol symbol, Long start, Long end, List<TradeBean> dataHolder) {
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
            try {
                trades = (CurrentAPI.get().aggTrades(symbol, limit, options));
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

                options.replace("startTime", end - 3600000L);
                options.replace("endTime", end);
            } catch (BinanceApiException e) {
                e.printStackTrace();
            }
        }
    }

}
