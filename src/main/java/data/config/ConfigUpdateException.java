package data.config;

public class ConfigUpdateException extends ConfigException {
    public ConfigUpdateException(String message) {
        super("Failed to update config: " + message);
    }
}
