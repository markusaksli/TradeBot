package data.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "name")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RsiData.class, name = "RSI"),
        @JsonSubTypes.Type(value = MacdData.class, name = "MACD"),
        @JsonSubTypes.Type(value = DbbData.class, name = "DBB")
})
public abstract class IndicatorData {
    private int weight;

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
