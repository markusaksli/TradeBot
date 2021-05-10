package data.config;

import indicators.Indicator;
import indicators.RSI;

import java.util.List;

public class RsiConfig extends IndicatorConfig {
    private int period;
    private int positiveMax;
    private int positiveMin;
    private int negativeMax;
    private int negativeMin;

    public RsiConfig(int weight, int period, int positiveMax, int positiveMin, int negativeMax, int negativeMin) {
        this.setWeight(weight);
        this.period = period;
        this.positiveMax = positiveMax;
        this.positiveMin = positiveMin;
        this.negativeMax = negativeMax;
        this.negativeMin = negativeMin;
    }

    public RsiConfig() {
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public int getPositiveMax() {
        return positiveMax;
    }

    public void setPositiveMax(int positiveMax) {
        this.positiveMax = positiveMax;
    }

    public int getPositiveMin() {
        return positiveMin;
    }

    public void setPositiveMin(int positiveMin) {
        this.positiveMin = positiveMin;
    }

    public int getNegativeMax() {
        return negativeMax;
    }

    public void setNegativeMax(int negativeMax) {
        this.negativeMax = negativeMax;
    }

    public int getNegativeMin() {
        return negativeMin;
    }

    public void setNegativeMin(int negativeMin) {
        this.negativeMin = negativeMin;
    }

    @Override
    public Indicator toIndicator(List<Double> warmupData) {
        return new RSI(warmupData, this);
    }

    @Override
    public void update(IndicatorConfig newConfig) throws ConfigUpdateException {
        super.update(newConfig);
        RsiConfig newRsiConfig = (RsiConfig) newConfig;
        if (newRsiConfig.period != period) {
            throw new ConfigUpdateException("RSI period has changed from " + period + " to " + newRsiConfig.period
                    + ". Period cannot be changed because exponential indicators are affeced by history.");
        }
        positiveMax = newRsiConfig.positiveMax;
        positiveMin = newRsiConfig.positiveMin;
        negativeMax = newRsiConfig.negativeMax;
        negativeMin = newRsiConfig.negativeMin;
    }

    @Override
    public String toString() {
        return "RSIData{" +
                "weight=" + getWeight() +
                ", period=" + period +
                ", positiveMax=" + positiveMax +
                ", positiveMin=" + positiveMin +
                ", negativeMax=" + negativeMax +
                ", negativeMin=" + negativeMin +
                '}';
    }
}
