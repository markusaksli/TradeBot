package collection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;

public class Database {
    static Connection con;

    static {
        try {
            con = DriverManager.getConnection("jdbc:sqlanywhere:uid=TradeBot;pwd=Andmebaasid2020;eng=bot");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void main(String[] args) throws SQLException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                con.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }));
        String path = "backtesting\\XRPUSDT_2020.01.01-2020.04.01.txt";
        String currency = new File(path).getName().split("_")[0];
        int currencyID = getCurrencyID(currency);
        try (PriceReader reader = new PriceReader(path)) {
            PriceBean bean = reader.readPrice();
            while (bean != null) {
                insertPriceBean(currencyID, bean.getTimestamp(), bean.getPrice());
                bean = reader.readPrice();
            }
        }
    }

    public static int getCurrencyID(String symbol) throws SQLException {
        int id;
        try (Statement st = con.createStatement()) {
            try (ResultSet rs = st.executeQuery("select id from currencies where symbol = '" + symbol + "'")) {
                rs.next();
                id = rs.getInt("id");
            }
        }
        return id;
    }

    public static int getIndicatorID(String name) throws SQLException {
        int id;
        try (Statement st = con.createStatement()) {
            try (ResultSet rs = st.executeQuery("select id from indicators where name = '" + name + "'")) {
                rs.next();
                id = rs.getInt("id");
            }
        }
        return id;
    }

    public static void insertPriceBean(int currency, long timestamp, double price) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeQuery(
                    "insert into currency_price_history(currency, price, \"timestamp\")" +
                            "values(" + currency + ", " + price + ", " + timestamp + ")");
        }
    }

    public static void insertIndicatorValue(String indicator, String currency, double value, long timestamp) throws SQLException {
        int indicatorID = getIndicatorID(indicator);
        int currencyID = getCurrencyID(currency);

        try (Statement st = con.createStatement()) {
            st.executeQuery(
                    "insert into indicator_history(indicator, currency, value, \"timestamp\")" +
                            "values(" + indicatorID + ", " + currencyID + ", " + value + ", " + timestamp + ")");
        }
    }
}
