package com.financial.tracker.financial_transactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class FinancialTransactionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialTransactionsApplication.class, args);
	}

}
