package data.config;

public class RsiData extends IndicatorData {
    private int period;
    private int positiveMax;
    private int positiveMin;
    private int negativeMax;
    private int negativeMin;

    public RsiData(int weight, int period, int positiveMax, int positiveMin, int negativeMax, int negativeMin) {
        this.setWeight(weight);
        this.period = period;
        this.positiveMax = positiveMax;
        this.positiveMin = positiveMin;
        this.negativeMax = negativeMax;
        this.negativeMin = negativeMin;
    }

    public RsiData() {
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
    public String toString() {
        return "RSIData{" +
                "weight=" + getWeight() +
                ", positiveMax=" + positiveMax +
                ", positiveMin=" + positiveMin +
                ", negativeMax=" + negativeMax +
                ", negativeMin=" + negativeMin +
                '}';
    }
}
