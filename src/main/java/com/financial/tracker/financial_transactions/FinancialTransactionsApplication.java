package com.financial.tracker.financial_transactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancialTransactionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialTransactionsApplication.class, args);
	}

}
