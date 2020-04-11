package Indicators;

import com.webcerebrium.binance.datatype.BinanceCandlestick;
import java.util.List;

public interface Indicator {

    double get();

    //Used in constructor to set initial value
    void setInitial(List<BinanceCandlestick> candles);

    //Used to update value
    void update(double newPrice);
}
