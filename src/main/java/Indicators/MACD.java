package Indicators;

import com.webcerebrium.binance.datatype.BinanceCandlestick;
import java.util.List;

//Default setting in crypto are period of 9, short 12 and long 26.
//Three parameters, MACD = 12 EMA - 26 EMA and compare to 9 EMA
public class MACD implements Indicator{
    private double currentMACD;
    private int shortPeriod;
    private int longPeriod;
    private int period;

    public MACD(int shortPeriod, int longPeriod, int period) {
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.period = period;
    }

    @Override
    public double get() {
        return 0;
    }

    @Override
    public void setInitial(List<BinanceCandlestick> candles) {

    }

    @Override
    public void update(double newPrice) {

    }
}
