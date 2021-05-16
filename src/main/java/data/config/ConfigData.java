package data.config;

import java.util.List;

public class ConfigData {
    private double moneyPerTrade;
    private double trailingSl;
    private double takeProfit;
    private int confluenceToOpen;
    private Integer confluenceToClose; //Number of indicators can't be changed

    private List<IndicatorConfig> indicators;

    public ConfigData(double moneyPerTrade, double trailingSl, double takeProfit, int confluenceToOpen, Integer confluenceToClose, List<IndicatorConfig> indicators) {
        this.moneyPerTrade = moneyPerTrade;
        this.trailingSl = trailingSl;
        this.takeProfit = takeProfit;
        this.confluenceToOpen = confluenceToOpen;
        this.confluenceToClose = confluenceToClose;
        this.indicators = indicators;
    }

    public ConfigData() {
    }

    public void update(ConfigData newConfig) throws ConfigUpdateException {
        if (newConfig.getIndicators().size() != indicators.size())
            throw new ConfigUpdateException("Number of indicators has changed");
        for (int i = 0; i < indicators.size(); i++) {
            indicators.get(i).update(newConfig.getIndicators().get(i));
        }
        moneyPerTrade = newConfig.moneyPerTrade;
        trailingSl = newConfig.trailingSl;
        takeProfit = newConfig.takeProfit;
        confluenceToOpen = newConfig.confluenceToOpen;
        confluenceToClose = newConfig.confluenceToClose;
    }

    public double getMoneyPerTrade() {
        return moneyPerTrade;
    }

    public void setMoneyPerTrade(double moneyPerTrade) {
        this.moneyPerTrade = moneyPerTrade;
    }

    public double getTrailingSl() {
        return trailingSl;
    }

    public void setTrailingSl(double trailingSl) {
        this.trailingSl = trailingSl;
    }

    public double getTakeProfit() {
        return takeProfit;
    }

    public void setTakeProfit(double takeProfit) {
        this.takeProfit = takeProfit;
    }

    public int getConfluenceToOpen() {
        return confluenceToOpen;
    }

    public void setConfluenceToOpen(int confluenceToOpen) {
        this.confluenceToOpen = confluenceToOpen;
    }

    public Integer getConfluenceToClose() {
        return confluenceToClose;
    }

    public void setConfluenceToClose(Integer confluenceToClose) {
        this.confluenceToClose = confluenceToClose;
    }

    public List<IndicatorConfig> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<IndicatorConfig> indicators) {
        this.indicators = indicators;
    }

    public boolean useConfluenceToClose() {
        return confluenceToClose != null;
    }

    @Override
    public String toString() {
        return "ConfigData{" +
                "indicators=" + indicators +
                ", moneyPerTrade=" + moneyPerTrade +
                ", trailingSl=" + trailingSl +
                ", takeProfit=" + takeProfit +
                ", confluenceToOpen=" + confluenceToOpen +
                ", confluenceToClose=" + confluenceToClose +
                '}';
    }
}
