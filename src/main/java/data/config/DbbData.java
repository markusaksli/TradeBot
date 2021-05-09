package data.config;

public class DbbData extends IndicatorData {
    private int period;

    public DbbData(int weight, int period) {
        setWeight(weight);
        this.period = period;
    }

    public DbbData() {
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    @Override
    public String toString() {
        return "DbbData{" +
                "weight=" + getWeight() +
                '}';
    }
}
