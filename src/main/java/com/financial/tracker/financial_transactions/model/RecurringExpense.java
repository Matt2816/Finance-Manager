package com.financial.tracker.financial_transactions.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "recurringExpense")
public class RecurringExpense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "merchant")
    private String merchant;

    @Column(name = "amount")
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency")
    private Frequency frequency;

    @Column(name = "startDate")
    private LocalDate startDate;

    @Column(name = "nextPaymentDate")
    private LocalDate nextPaymentDate;

    public RecurringExpense(String merchant, Double amount, Frequency frequency, LocalDate startDate, LocalDate nextPaymentDate) {
        this.merchant = merchant;
        this.amount = amount;
        this.frequency = frequency;
        this.startDate = startDate;
        this.nextPaymentDate = nextPaymentDate;
    }

    public RecurringExpense() {}

    public int getId() {return id;}

    public void setId(int id) {this.id = id;}

    public String getMerchant() {return merchant;}

    public void setMerchant(String merchant) {this.merchant = merchant;}

    public Double getAmount() {return amount;}

    public void setAmount(Double amount) {this.amount = amount;}

    public Frequency getFrequency() {return frequency;}

    public void setFrequency(Frequency frequency) {this.frequency = frequency;}

    public LocalDate getStartDate() {return startDate;}

    public void setStartDate(LocalDate startDate) {this.startDate = startDate;}

    public LocalDate getNextPaymentDate() {return nextPaymentDate;}

    public void setNextPaymentDate(LocalDate nextPaymentDate) {this.nextPaymentDate = nextPaymentDate;}

    @Override
    public String toString() {
        return "RecurringExpense{" +
                "id=" + id +
                ", merchant='" + merchant + '\'' +
                ", amount=" + amount +
                ", frequency=" + frequency +
                ", startDate=" + startDate +
                ", nextPaymentDate=" + nextPaymentDate +
                '}';
    }
}

