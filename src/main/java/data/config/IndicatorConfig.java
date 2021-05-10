package data.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import indicators.Indicator;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "name")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RsiConfig.class, name = "RSI"),
        @JsonSubTypes.Type(value = MacdConfig.class, name = "MACD"),
        @JsonSubTypes.Type(value = DbbConfig.class, name = "DBB")
})
public abstract class IndicatorConfig {
    private int weight;

    public abstract Indicator toIndicator(List<Double> warmupData);

    //Subclasses must call super if overriding
    public void update(IndicatorConfig newConfig) throws ConfigUpdateException {
        if (newConfig.getClass() != getClass()) throw new ConfigUpdateException("Indicator order has changed");
        weight = newConfig.weight;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {

        System.out.println(getClass());
        this.weight = weight;
    }
}
