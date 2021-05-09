package system;

import com.binance.api.client.domain.general.RateLimit;
import com.binance.api.client.domain.general.RateLimitType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import data.config.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Config {
    private static final int REQUEST_LIMIT = BinanceAPI.get().getExchangeInfo().getRateLimits().stream()
            .filter(rateLimit -> rateLimit.getRateLimitType().equals(RateLimitType.REQUEST_WEIGHT))
            .findFirst().map(RateLimit::getLimit).orElse(1200);

    public static void main(String[] args) {
        File file = new File("config.yaml");
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        Set<IndicatorData> set = new HashSet<>();
        ConfigData crazyConfig = new ConfigData(
                0.1,
                0.1,
                0.15,
                2,
                null,
                Arrays.asList(new RsiData(1, 14, 15, 30, 70, 80), new MacdData(1, 12, 26, 9, 0.15), new DbbData(1, 20))
        );
        try {
            System.out.println(objectMapper.writeValueAsString(crazyConfig));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        ConfigData config = null;
        try {
            config = objectMapper.readValue(file, ConfigData.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(config.toString());
    }

    private final File configFile;
    private ConfigData data;

    public Config(String path) throws ConfigException {
        configFile = new File(path);
        readValues();
    }

    public static int getRequestLimit() {
        return REQUEST_LIMIT;
    }

    public ConfigData getData() {
        return data;
    }

    public void readValues() throws ConfigException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            data = objectMapper.readValue(configFile, ConfigData.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to read config file due to: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "Failed to serialize config: " + e.getMessage();
        }
    }
}
