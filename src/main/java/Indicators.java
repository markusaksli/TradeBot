import com.webcerebrium.binance.api.BinanceApi;
import com.webcerebrium.binance.api.BinanceApiException;
import com.webcerebrium.binance.datatype.BinanceCandlestick;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;


import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Indicators {
    private static final MathContext context = new MathContext(10, RoundingMode.HALF_EVEN);

    public static int getRSI(List<BinanceCandlestick> candles, int period) {
        //Cant calculate so return invalid value
        if (period > candles.size()) {
            return -1;
        }

        List<BigDecimal> upList = new ArrayList<>();
        List<BigDecimal> dwnList = new ArrayList<>();
        for (int i = candles.size() - 1; i >= candles.size() - period; i--) {
            BinanceCandlestick candle = candles.get(i);
            BigDecimal change = candle.getClose().subtract(candle.getOpen());
            if (change.signum() == -1) {
                dwnList.add(change.abs());
            } else {
                upList.add(change);
            }
        }
        BigDecimal avgUp = average(upList, period);
        BigDecimal avgDwn = average(dwnList, period);
        BigDecimal division = avgUp.divide(avgDwn, context);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(BigDecimal.valueOf(1).add(division), context));
        System.out.println("RSI exact: " + rsi.toString());

        return rsi.intValue();
    }

    public static BigDecimal average(List<BigDecimal> prices, int period) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal price : prices) {
            sum = sum.add(price);
        }
        return sum.divide(BigDecimal.valueOf(period), context);
    }

    //Default setting in crypto are period of 9, short 12 and long 26.
    //Three parameters, MACD = 12 EMA - 26 EMA and compare to 9 EMA
    public static double getEMA(List<BinanceCandlestick> candles, int period) {
        if (period > candles.size()) return -1;

        double previousSMA = 0;

        for (int i = 0; i < period; i++) {
            previousSMA += (candles.get(i).close.doubleValue());
        }
        double multiplier = 2 / ((double) period + 1);
        previousSMA = previousSMA / period;

        double EMA = previousSMA;
        for (int i = period + 1; i < candles.size(); i++) {
            // EMA = (Close - EMApreviousDay) * multiplier + EMApreviousDay
            EMA = (candles.get(i).close.doubleValue() - EMA) * multiplier + EMA;
        }

        return EMA;


    }

}
