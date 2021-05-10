package data.config;

import indicators.Indicator;
import indicators.MACD;

import java.util.List;

public class MacdConfig extends IndicatorConfig {
    private int shortPeriod;
    private int longPeriod;
    private int signalPeriod;
    private double requiredChange;

    public MacdConfig(int weight, int shortPeriod, int longPeriod, int signalPeriod, double requiredChange) {
        this.setWeight(weight);
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.signalPeriod = signalPeriod;
        this.requiredChange = requiredChange;
    }

    public MacdConfig() {
    }

    public int getShortPeriod() {
        return shortPeriod;
    }

    public void setShortPeriod(int shortPeriod) {
        this.shortPeriod = shortPeriod;
    }

    public int getLongPeriod() {
        return longPeriod;
    }

    public void setLongPeriod(int longPeriod) {
        this.longPeriod = longPeriod;
    }

    public int getSignalPeriod() {
        return signalPeriod;
    }

    public void setSignalPeriod(int signalPeriod) {
        this.signalPeriod = signalPeriod;
    }

    public double getRequiredChange() {
        return requiredChange;
    }

    public void setRequiredChange(double requiredChange) {
        this.requiredChange = requiredChange;
    }

    @Override
    public Indicator toIndicator(List<Double> warmupData) {
        return new MACD(warmupData, this);
    }

    @Override
    public void update(IndicatorConfig newConfig) throws ConfigUpdateException {
        super.update(newConfig);
        MacdConfig newMacdConfig = (MacdConfig) newConfig;
        if (newMacdConfig.longPeriod != longPeriod) {
            throw new ConfigUpdateException("MACD long period has changed from " + longPeriod + " to " + newMacdConfig.longPeriod
                    + ". Period cannot be changed because exponential indicators are affeced by history.");
        }
        if (newMacdConfig.shortPeriod != shortPeriod) {
            throw new ConfigUpdateException("MACD short period has changed from " + shortPeriod + " to " + newMacdConfig.shortPeriod
                    + ". Period cannot be changed because exponential indicators are affeced by history.");
        }
        if (newMacdConfig.signalPeriod != signalPeriod) {
            throw new ConfigUpdateException("MACD signal period has changed from " + signalPeriod + " to " + newMacdConfig.signalPeriod
                    + ". Period cannot be changed because exponential indicators are affeced by history.");
        }
        requiredChange = newMacdConfig.requiredChange;
    }

    @Override
    public String toString() {
        return "MacdConfig{" +
                "weight=" + getWeight() +
                ", shortPeriod=" + shortPeriod +
                ", longPeriod=" + longPeriod +
                ", signalPeriod=" + signalPeriod +
                ", requiredChange=" + requiredChange +
                '}';
    }
}
