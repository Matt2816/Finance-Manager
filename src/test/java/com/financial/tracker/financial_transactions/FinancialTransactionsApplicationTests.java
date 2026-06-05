package com.financial.tracker.financial_transactions;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.financial.tracker.financial_transactions.config.TestSecurityConfig;

@SpringBootTest
@Import(TestSecurityConfig.class)
class FinancialTransactionsApplicationTests {

	@Test
	void contextLoads() {
	}

}
