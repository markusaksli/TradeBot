package data.config;

public class MacdData extends IndicatorData {
    private int shortPeriod;
    private int longPeriod;
    private int signalPeriod;
    private double requiredChange;

    public MacdData(int weight, int shortPeriod, int longPeriod, int signalPeriod, double requiredChange) {
        this.setWeight(weight);
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.signalPeriod = signalPeriod;
        this.requiredChange = requiredChange;
    }

    public MacdData() {
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
    public String toString() {
        return "MacdData{" +
                "requiredChange=" + requiredChange +
                '}';
    }
}
