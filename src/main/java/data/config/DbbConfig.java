package data.config;

import indicators.DBB;
import indicators.Indicator;

import java.util.List;

public class DbbConfig extends IndicatorConfig {
    private int period;

    public DbbConfig(int weight, int period) {
        setWeight(weight);
        this.period = period;
    }

    public DbbConfig() {
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    @Override
    public Indicator toIndicator(List<Double> warmupData) {
        return new DBB(warmupData, this);
    }

    @Override
    public void update(IndicatorConfig newConfig) throws ConfigUpdateException {
        super.update(newConfig);
        DbbConfig newDbbConfig = (DbbConfig) newConfig;
        if (newDbbConfig.getPeriod() != period) {
            throw new ConfigUpdateException("DBB period has changed from " + period + " to " + newDbbConfig.period
                    + ". Period cannot be changed because DBB values are affected by the size of the rolling window.");
        }
    }

    @Override
    public String toString() {
        return "DbbData{" +
                "weight=" + getWeight() +
                "period=" + period +
                '}';
    }
}
