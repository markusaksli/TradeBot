package Indicators;

import com.webcerebrium.binance.datatype.BinanceCandlestick;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class RSI {
    private static final MathContext context = new MathContext(10, RoundingMode.HALF_UP);

    public static int getRSI(List<BinanceCandlestick> candles, int period) {
        if (period > candles.size()) {
            return -1;
        } else {
            System.out.println(((BinanceCandlestick) candles.get(0)).getClose());
            List<BigDecimal> upList = new ArrayList();
            List<BigDecimal> dwnList = new ArrayList();
            BigDecimal prevClose = ((BinanceCandlestick) candles.get(candles.size() - period - 1)).getClose();

            BigDecimal change;
            for (int i = candles.size() - period; i < candles.size(); ++i) {
                BinanceCandlestick candle = (BinanceCandlestick) candles.get(i);
                change = candle.getClose().subtract(prevClose);
                if (change.signum() == -1) {
                    dwnList.add(change.abs());
                } else {
                    upList.add(change);
                }

                prevClose = candle.getClose();
            }

            BigDecimal avgUp = average(upList, period);
            BigDecimal avgDwn = average(dwnList, period);
            change = avgUp.divide(avgDwn, context);
            BigDecimal rsi = BigDecimal.valueOf(100L).subtract(BigDecimal.valueOf(100L).divide(BigDecimal.valueOf(1L).add(change), context));
            System.out.println("RSI exact: " + rsi.toString());
            return rsi.intValue();
        }
    }

    public static BigDecimal average(List<BigDecimal> prices, int period) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal price : prices) {
            sum = sum.add(price);
        }
        return sum.divide(BigDecimal.valueOf(period), context);
    }
}
