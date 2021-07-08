package data.config;

import com.binance.api.client.domain.general.RateLimit;
import com.binance.api.client.domain.general.RateLimitType;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import system.BinanceAPI;
import trading.Currency;
import trading.LocalAccount;
import trading.Trade;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Config {
    private static final int REQUEST_LIMIT = BinanceAPI.get().getExchangeInfo().getRateLimits().stream()
            .filter(rateLimit -> rateLimit.getRateLimitType().equals(RateLimitType.REQUEST_WEIGHT))
            .findFirst().map(RateLimit::getLimit).orElse(1200);

    private final File configFile;
    private final ConfigData data;

    public Config(String path) throws ConfigException {
        configFile = new File(path);
        data = readValues();
    }

    public String name() {
        return configFile.getName();
    }

    public static ConfigData get(Trade trade) {
        return trade.getCurrency().getAccount().getInstance().getConfig();
    }

    public static ConfigData get(Currency currency) {
        return currency.getAccount().getInstance().getConfig();
    }

    public static ConfigData get(LocalAccount account) {
        return account.getInstance().getConfig();
    }

    public static int getRequestLimit() {
        return REQUEST_LIMIT;
    }

    public ConfigData getData() {
        return data;
    }

    public ConfigData readValues() throws ConfigException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            return objectMapper.readValue(configFile, ConfigData.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to read config file due to: " + e.getMessage());
        }
    }

    public void update() throws ConfigException {
        data.update(readValues());
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "Failed to serialize config: " + e.getMessage();
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
