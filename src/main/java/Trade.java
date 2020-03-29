import java.sql.Timestamp;

public class Trade {

    private final int entryPrice; //Strarting price of a trade (when logic decides to buy)
    private int fillPrice; //The actual price after the completion of a fill
    private final Timestamp logicTime; // When the programs logic decides to make a trade
    private  Timestamp acceptTime; // When the server gets the signal of a trade
    private Timestamp fillTime; //When the fill is completed


    //Can get all of the data straight away
    public Trade(double entryPrice, double fillPrice, Timestamp logicTime, Timestamp acceptTime, Timestamp fillTime) {
        this(entryPrice, logicTime, acceptTime);
        this.fillPrice = fillPrice;//The prices are wrong right now because we need to turn the prices to int-s
        this.fillTime = fillTime;
    }

    public Trade(double entryPrice, Timestamp logicTime) { //Can't get server accept time and fill price/time
        this.entryPrice = entryPrice;
        this.logicTime = logicTime;
    }

    public Trade(double entryPrice, Timestamp logicTime, Timestamp acceptTime) { //Can't get data about fillPrice and time
        this(entryPrice,logicTime);
        this.acceptTime = acceptTime;
    }

    //Getters and setters

    public void setFillPrice(double fillPrice) {
        this.fillPrice = fillPrice;
    }

    public void setFillTime(Timestamp fillTime) {
        this.fillTime = fillTime;
    }

    public void setAcceptTime(Timestamp acceptTime) {
        this.acceptTime = acceptTime;
    }

    public int getEntryPrice() {
        return entryPrice;
    }

    public int getFillPrice() {
        return fillPrice;
    }

    public Timestamp getLogicTime() {
        return logicTime;
    }

    public Timestamp getAcceptTime() {
        return acceptTime;
    }

    public Timestamp getFillTime() {
        return fillTime;
    }
}
