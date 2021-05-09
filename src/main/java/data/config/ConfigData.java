package data.config;

import java.util.List;

public class ConfigData {
    private double moneyPerTrade;
    private double trailingSl;
    private double takeProfit;
    private int confluenceToOpen;
    private Integer confluenceToClose;

    private List<IndicatorData> indicators;

    public ConfigData(double moneyPerTrade, double trailingSl, double takeProfit, int confluenceToOpen, Integer confluenceToClose, List<IndicatorData> indicators) {
        this.moneyPerTrade = moneyPerTrade;
        this.trailingSl = trailingSl;
        this.takeProfit = takeProfit;
        this.confluenceToOpen = confluenceToOpen;
        this.confluenceToClose = confluenceToClose;
        this.indicators = indicators;
    }

    public ConfigData() {
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

    public List<IndicatorData> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<IndicatorData> indicators) {
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
