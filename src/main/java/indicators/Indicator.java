package indicators;

import java.util.List;

public interface Indicator {

    //Used to get the latest indicator value updated with closed candle
    double get();

    //Used to get value of indicator simulated with the latest non-closed price
    double getTemp(double newPrice);

    //Used in constructor to set initial value
    void init(List<Double> closingPrices);

    //Used to update value with latest closed candle closing price
    void update(double newPrice);

    //Used to check for buy signal
    int check(double newPrice);

    String getExplanation();
}
