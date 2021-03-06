package se.kth.iv1350.pos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import se.kth.iv1350.pos.integration.*;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * This represents the process of a sale and the required actions.
 * @author Alexander Lundqvist
 */
public class Sale {
    private LocalTime timeOfSale;
    private LocalDate dateOfSale;
    private Receipt receipt;
    private Printer printer;
    private List<GroceryItem> addedItems = new ArrayList<>();
    private Discount discount;
    private double runningTotal;
    private double totalVAT;
    private double change;
    private SaleDTO saleInfo;
    private List<SaleObserver> observerList = new ArrayList<>();
    
    /**
     * Creates a new instance and saves the time of the sale.
     * @param printer is the newly created printer
     */
    public Sale(Printer printer) {
        this.timeOfSale = LocalTime.now();
        this.dateOfSale = LocalDate.now();
        this.runningTotal = 0;
        this.totalVAT = 0;
        this.printer = printer;
    }
    
    /**
     * Adding an {@link GroceryItemDTO} to the sale and also calling for price update.
     * @param item The item to be added
     * @param quantity how many of said item
     */
    public void addItem(GroceryItemDTO item, int quantity){
        if(checkExistingItem(item, quantity) == true){
           return;
        }    
        else {
            addedItems.add(new GroceryItem(item, quantity));
            updateRunningTotal(item, quantity);
            updateTotalVAT(item, quantity);
        }
    }
    
    private boolean checkExistingItem(GroceryItemDTO item, int quantity) {
        for (int i = 0; i < addedItems.size(); i++){
            if (addedItems.get(i).getItemIdentifier() == item.getItemIdentifier()){
                addedItems.get(i).setQuantity(quantity + addedItems.get(i).getQuantity());
                updateRunningTotal(item, quantity);
                updateTotalVAT(item, quantity);
                return true;
            }
        }
        return false;
    }
    
    private void updateRunningTotal(GroceryItemDTO item, int quantity) {
        runningTotal += new BigDecimal((item.getPrice()*quantity)*((item.getVAT()/100)+1)).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
    
    
    private void updateTotalVAT(GroceryItemDTO item, int quantity) {
        totalVAT += new BigDecimal((item.getPrice()*quantity)*(item.getVAT()/100)).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
    
    /**
     * Standard getter for retrieveing the running total of the sale
     * @return the running total
     */
    public double getRunningTotal() {
        this.runningTotal = change = (double) Math.round((this.runningTotal) * 100) / 100;
        return this.runningTotal;
    }
    
    /**
     * Standard getter for the total VAT of the sale
     * @return the total VAT
     */
    public double getTotalVAT() {
        return this.totalVAT;
    }
   
    
    private void updateChange(double amountPaid){
        change = (double) Math.round((amountPaid - getRunningTotal()) * 100) / 100;
    }
    
    /**
     * Creates a new discount for the sale and applies it
     * @param customerID 
     */
    public void addDiscount(String customerID) {
        this.discount = new Discount(customerID, addedItems);
        this.runningTotal *= discount.calculateTotalDiscount();
    }
    
    /**
     * Ends the current sale
     * @param amountPaid is the amount that the customer paid
     * @return the {@link SaleDTO}
     */
    public SaleDTO endSale(double amountPaid) {
        updateChange(amountPaid);
        saleInfo = new SaleDTO(timeOfSale, dateOfSale, addedItems, 
                runningTotal, totalVAT, amountPaid, change);
        notifyObservers();
        return saleInfo;
    }
    
    /**
     * Creates a receipt and also sends it to the printer
     */
    public void createReceipt(){
        receipt = new Receipt(saleInfo);
        printer.printReceipt(receipt);
    }
    
    /**
     * Adds an observewr to the observer list
     * @param observer 
     */
    public void addObservers(List<SaleObserver> observers) {
        observerList.addAll(observers);
    }
    
    
    private void notifyObservers() {
        for (SaleObserver observer : observerList) {
            observer.newRunningTotal(getRunningTotal());
        }
    }
}
