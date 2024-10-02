package com.financial.tracker.financial_transactions.model;

import jakarta.persistence.*;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "cardtype")
    private String cardType;
    @Column(name = "amount")
    private String amount;
    @Column(name = "name")
    private String name;
    @Column(name = "merchant")
    private String merchant;
    @Column(name = "transactiondate")
    private String transactionDate;
    @Column(name = "address")
    private String address;
    @Column(name = "hash")
    private String hash;

    public Transaction(String cardType, String amount, String name, String merchant, String transactionDate, String address, String hash) {
        this.cardType = cardType;
        this.amount = amount;
        this.name = name;
        this.merchant = merchant;
        this.transactionDate = transactionDate;
        this.address = address;
        this.hash = hash;
    }
    public Transaction() {}


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getHash() {return hash;}

    public void setHash(String hash) {this.hash = hash;}

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", cardType='" + cardType + '\'' +
                ", amount='" + amount + '\'' +
                ", name='" + name + '\'' +
                ", merchant='" + merchant + '\'' +
                ", transactionDate='" + transactionDate + '\'' +
                ", address='" + address + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }



}
