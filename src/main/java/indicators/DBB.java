package indicators;
import data.config.DbbConfig;

import java.util.List;

//Common period for BB is 20
public class DBB implements Indicator {
    private final DbbConfig config;

    private double closingPrice;
    private double standardDeviation;
    private double upperBand;
    private double upperMidBand;
    private double middleBand;
    private double lowerMidBand;
    private double lowerBand;
    private String explanation;
    private SMA sma;

    public DBB(List<Double> warmupData, DbbConfig config) {
        this.config = config;
        this.sma = new SMA(warmupData, config.getPeriod());
        init(warmupData);
    }

    @Override
    public double get() {
        if ((upperBand - lowerBand) / middleBand < 0.05) //Low volatility case
            return 0;
        if (upperMidBand < closingPrice && closingPrice <= upperBand)
            return 1;
        if (lowerBand < closingPrice && closingPrice <= lowerMidBand)
            return -1;
        else
            return 0;
    }

    @Override
    public double getTemp(double newPrice) {
        double tempMidBand = sma.getTemp(newPrice);
        double tempStdev = sma.tempStandardDeviation(newPrice);
        double tempUpperBand = tempMidBand + tempStdev * 2;
        double tempUpperMidBand = tempMidBand + tempStdev;
        double tempLowerMidBand = tempMidBand - tempStdev;
        double tempLowerBand = tempMidBand - tempStdev * 2;
        if ((tempUpperBand - tempLowerBand) / tempMidBand < 0.05) //Low volatility case
            return 0;
        if (tempUpperMidBand < newPrice && newPrice <= tempUpperBand)
            return 1;
        if (tempLowerBand < newPrice && newPrice <= tempLowerMidBand)
            return -1;
        else
            return 0;
    }

    @Override
    public void init(List<Double> warmupData) {
        if (config.getPeriod() > warmupData.size()) return;

        closingPrice = warmupData.size() - 2;
        standardDeviation = sma.standardDeviation();
        middleBand = sma.get();
        upperBand = middleBand + standardDeviation * 2;
        upperMidBand = middleBand + standardDeviation;
        lowerMidBand = middleBand - standardDeviation;
        lowerBand = middleBand - standardDeviation * 2;

    }

    @Override
    public void update(double newPrice) {
        closingPrice = newPrice;
        sma.update(newPrice);
        standardDeviation = sma.standardDeviation();
        middleBand = sma.get();
        upperBand = middleBand + standardDeviation*2;
        upperMidBand = middleBand + standardDeviation;
        lowerMidBand = middleBand - standardDeviation;
        lowerBand = middleBand - standardDeviation*2;
    }

    @Override
    public int check(double newPrice) {
        if (getTemp(newPrice) == 1) {
            explanation = "Price in DBB buy zone";
            return config.getWeight();
        }
        if (getTemp(newPrice) == -1) {
            explanation = "Price in DBB sell zone";
            return -config.getWeight();
        }
        explanation = "";
        return 0;
    }

    @Override
    public String getExplanation() {
        return explanation;
    }
}
