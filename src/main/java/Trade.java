import java.sql.Timestamp;

public class Trade {

    private double highestPrice; //Set the highest price
    private double stopLoss; //It's in percentages, but using double for comfort.
    private double entryPrice; //Starting price of a trade (when logic decides to buy)
    //private double fillPrice; //The actual price after the completion of a fill
    private final Timestamp logicTime; // When the programs logic decides to make a trade
    private  Timestamp acceptTime; // When the server gets the signal of a trade
    private Timestamp fillTime; //When the fill is completed.
    private Currency currency; //What cryptocurrency is used.
    private double amountOfCurrency; //How much are you buying or selling. I.E 06 bitcoins or smth.
    private double closePrice;


    //Can get all of the data straight away
    //Biggest constructor
    public Trade(Currency currency,double entryPrice/*, double fillPrice */,Timestamp logicTime,  double amountOfCurrency, Timestamp acceptTime /*, Timestamp fillTime*/) {
        this(entryPrice, logicTime, amountOfCurrency, acceptTime); //References the cunstructor below
        this.currency = currency;
        //this.fillPrice = fillPrice;
        //this.fillTime = fillTime;

    }
    //Medium constructor
    public Trade(double entryPrice, Timestamp logicTime, double amountOfCurrency, Timestamp acceptTime) { //Can't get data about fillPrice and time
        this(entryPrice,logicTime, amountOfCurrency); //References the constructor below
        this.acceptTime = acceptTime;
    }
    //Smallest constructor
    public Trade(double entryPrice, Timestamp logicTime, double amountOfCurrency) { //Can't get server accept time and fill price/time
        this.entryPrice = entryPrice;
        this.logicTime = logicTime;
        this.amountOfCurrency = amountOfCurrency;
        this.highestPrice = entryPrice; //Might need to replace it with fillPrice in the future.
    }


    //Getters and setters
    public void setClosePrice(double closePrice) {
        this.closePrice = closePrice;
    }

    public double getClosePrice() {
        return closePrice;
    }

    /*
    public void setFillPrice(double fillPrice) {
        this.fillPrice = fillPrice;
    }

     */

    public Currency getCurrency() { //for getting the currency to calculate what the price is now.
        return currency;
    }

    public double getAmountOfCurrency() {
        return amountOfCurrency;
    }

    public void setFillTime(Timestamp fillTime) {
        this.fillTime = fillTime;
    }

    public void setAcceptTime(Timestamp acceptTime) {
        this.acceptTime = acceptTime;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    /*
    public double getFillPrice() {
        return fillPrice;
    }

     */

    public Timestamp getLogicTime() {
        return logicTime;
    }

    public Timestamp getAcceptTime() {
        return acceptTime;
    }

    public Timestamp getFillTime() {
        return fillTime;
    }

    //Allows user to get the profit percentages on one specific trade.
    public double getProfitUSD() {
        double priceNow = currency.getPrice();
        double percentages = Math.round((priceNow * 100 / entryPrice - 100) * 1000); //Replaced fillPrice with entryPrice since we're not focusing on fillPrice right now at all.
        percentages = percentages / 1000;
        return percentages;
    }

    //Checks if there is a new highest price for the trade or if the trade has dropped below the stoploss.
    public void update (double newPrice){
        if (newPrice > highestPrice)
            highestPrice = newPrice;
        else if (newPrice < highestPrice * (1 - stopLoss)) {
            //Close the trade
        }
    }
}
