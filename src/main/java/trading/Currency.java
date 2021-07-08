package trading;

import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import data.config.IndicatorConfig;
import data.price.PriceBean;
import data.price.PriceReader;
import indicators.Indicator;
import system.BinanceAPI;
import data.config.Config;
import system.Formatter;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Currency implements Closeable {
    private final String pair;
    private LocalAccount account;
    private Trade activeTrade;
    private long candleTime;
    private final List<Indicator> indicators = new ArrayList<>();
    private final AtomicBoolean currentlyCalculating = new AtomicBoolean(false);

    private double currentPrice;
    private long currentTime;

    //Backtesting data
    private final StringBuilder log = new StringBuilder();
    private PriceBean firstBean;

    private Closeable apiListener;

    //Used for SIMULATION and LIVE
    public Currency(String coin, LocalAccount account) {
        this.pair = coin + account.getInstance().getFiat();
        this.account = account;

        //Every currency needs to contain and update our indicators
        List<Candlestick> history = BinanceAPI.get().getCandlestickBars(pair, CandlestickInterval.FIVE_MINUTES);
        List<Double> indicatorWarmupPrices = history.stream().map(candle -> Double.parseDouble(candle.getClose())).collect(Collectors.toList());
        for (IndicatorConfig indicatorConfig : Config.get(this).getIndicators()) {
            indicators.add(indicatorConfig.toIndicator(indicatorWarmupPrices));
        }

        //We set the initial values to check against in onMessage based on the latest candle in history
        currentTime = System.currentTimeMillis();
        candleTime = history.get(history.size() - 1).getCloseTime();
        currentPrice = Double.parseDouble(history.get(history.size() - 1).getClose());

        BinanceApiWebSocketClient client = BinanceAPI.getFactory().newWebSocketClient();
        //We add a websocket listener that automatically updates our values and triggers our strategy or trade logic as needed
        apiListener = client.onAggTradeEvent(pair.toLowerCase(), response -> {
            double newPrice = Double.parseDouble(response.getPrice());
            long newTime = response.getEventTime();

            //We want to toss messages that provide no new information
            if (currentPrice == newPrice && newTime <= candleTime) {
                return;
            }

            if (newTime > candleTime) {
                accept(new PriceBean(candleTime, currentPrice, true));
                candleTime += 300000L;
            }

            accept(new PriceBean(newTime, newPrice));
        });
        System.out.println("---SETUP DONE FOR " + this);
    }

    //Used for BACKTESTING
    public Currency(PriceReader reader, LocalAccount account) {
        this.pair = reader.getPair();
        this.account = account;
    }

    public CompletableFuture<Void> runBacktest(PriceReader reader) {
        try (reader) {
            PriceBean bean = reader.readPrice();

            firstBean = bean;
            List<Double> indicatorWarmupPrices = new ArrayList<>();
            while (bean.isClosing()) {
                indicatorWarmupPrices.add(bean.getPrice());
                bean = reader.readPrice();
            }
            for (IndicatorConfig indicatorConfig : Config.get(this).getIndicators()) {
                indicators.add(indicatorConfig.toIndicator(indicatorWarmupPrices));
            }
            while (bean != null) {
                accept(bean);
                bean = reader.readPrice();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }

    private void accept(PriceBean bean) {
        //Make sure we dont get concurrency issues
        if (currentlyCalculating.get()) {
            System.out.println("------------WARNING, NEW THREAD STARTED ON " + pair + " MESSAGE DURING UNFINISHED PREVIOUS MESSAGE CALCULATIONS");
        }

        currentPrice = bean.getPrice();
        currentTime = bean.getTimestamp();

        if (bean.isClosing()) {
            indicators.forEach(indicator -> indicator.update(bean.getPrice()));
            if (account.getInstance().getMode().equals(Instance.Mode.BACKTESTING)) {
                appendLogLine(system.Formatter.formatDate(currentTime) + " " + this);
            }
        }

        if (!currentlyCalculating.get()) {
            int confluence = 0; //0 Confluence should be reserved in the config for doing nothing
            currentlyCalculating.set(true);
            //We can disable the strategy and trading logic to only check indicator and price accuracy
            if ((Config.get(this).useConfluenceToClose() && hasActiveTrade()) || account.enoughFunds()) {
                confluence = check();
            }
            if (hasActiveTrade()) { //We only allow one active trade per currency, this means we only need to do one of the following:
                activeTrade.update(currentPrice, confluence);//Update the active trade stop-loss and high values
            } else if (confluence >= Config.get(this).getConfluenceToOpen() && account.enoughFunds()) {
                account.open(Currency.this, "Trade opened due to: " + getExplanations());
            }
            currentlyCalculating.set(false);
        }
    }

    public int check() {
        return indicators.stream().mapToInt(indicator -> indicator.check(currentPrice)).sum();
    }

    public String getExplanations() {
        StringBuilder builder = new StringBuilder();
        for (Indicator indicator : indicators) {
            String explanation = indicator.getExplanation();
            if (explanation == null) explanation = "";
            builder.append(explanation.equals("") ? "" : explanation + "\t");
        }
        return builder.toString();
    }

    public String getPair() {
        return pair;
    }

    public double getPrice() {
        return currentPrice;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public boolean hasActiveTrade() {
        return activeTrade != null;
    }

    public void setActiveTrade(Trade activeTrade) {
        this.activeTrade = activeTrade;
    }

    public Trade getActiveTrade() {
        return activeTrade;
    }

    public void appendLogLine(String s) {
        log.append(s).append("\n");
    }

    public LocalAccount getAccount() {
        return account;
    }

    public void log(String path) {
        List<Trade> tradeHistory = new ArrayList<>(account.getTradeHistory());
        try (FileWriter writer = new FileWriter(path)) {
            writer.write("Test ended " + system.Formatter.formatDate(LocalDateTime.now()) + " \n");
            writer.write("\n\nCONFIG:\n");
            writer.write(Config.get(this).toString());
            writer.write("\n\nMarket performance: " + system.Formatter.formatPercent((currentPrice - firstBean.getPrice()) / firstBean.getPrice()));
            if (!tradeHistory.isEmpty()) {
                tradeHistory.sort(Comparator.comparingDouble(Trade::getProfit));
                double maxLoss = tradeHistory.get(0).getProfit();
                double maxGain = tradeHistory.get(tradeHistory.size() - 1).getProfit();
                int lossTrades = 0;
                double lossSum = 0;
                int gainTrades = 0;
                double gainSum = 0;
                long tradeDurs = 0;
                for (Trade trade : tradeHistory) {
                    double profit = trade.getProfit();
                    if (profit < 0) {
                        lossTrades += 1;
                        lossSum += profit;
                    } else if (profit > 0) {
                        gainTrades += 1;
                        gainSum += profit;
                    }
                    tradeDurs += trade.getDuration();
                }

                double tradePerWeek = 604800000.0 / (((double) currentTime - firstBean.getTimestamp()) / tradeHistory.size());

                writer.write("\nBot performance: " + system.Formatter.formatPercent(account.getProfit()) + "\n\n");
                writer.write(account.getTradeHistory().size() + " closed trades"
                        + " (" + system.Formatter.formatDecimal(tradePerWeek) + " trades per week) with an average holding length of "
                        + system.Formatter.formatDuration(Duration.of(tradeDurs / tradeHistory.size(), ChronoUnit.MILLIS)) + " hours");
                if (lossTrades != 0) {
                    writer.write("\nLoss trades:\n");
                    writer.write(lossTrades + " trades, " + system.Formatter.formatPercent(lossSum / (double) lossTrades) + " average, " + system.Formatter.formatPercent(maxLoss) + " max");
                }
                if (gainTrades != 0) {
                    writer.write("\nProfitable trades:\n");
                    writer.write(gainTrades + " trades, " + system.Formatter.formatPercent(gainSum / (double) gainTrades) + " average, " + system.Formatter.formatPercent(maxGain) + " max");
                }
                writer.write("\n\nClosed trades (least to most profitable):\n");
                for (Trade trade : tradeHistory) {
                    writer.write(trade.toString() + "\n");
                }
            } else {
                writer.write("\n(Not trades made)\n");
                System.out.println("---No trades made in the time period!");
            }
            writer.write("\n\nFULL LOG:\n\n");
            writer.write(log.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("---Log file generated at " + new File(path).getAbsolutePath());
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(pair + " price: " + currentPrice);
        if (currentTime == candleTime)
            indicators.forEach(indicator -> s.append(", ").append(indicator.getClass().getSimpleName()).append(": ").append(system.Formatter.formatDecimal(indicator.get())));
        else
            indicators.forEach(indicator -> s.append(", ").append(indicator.getClass().getSimpleName()).append(": ").append(Formatter.formatDecimal(indicator.getTemp(currentPrice))));
        s.append(", hasActive: ").append(hasActiveTrade()).append(")");
        return s.toString();
    }

    @Override
    public int hashCode() {
        return pair.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj.getClass() != Currency.class) return false;
        return pair.equals(((Currency) obj).pair);
    }

    @Override
    public void close() throws IOException {
        if (apiListener != null) apiListener.close();
    }
}
