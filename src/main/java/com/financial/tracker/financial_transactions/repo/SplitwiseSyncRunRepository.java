package com.financial.tracker.financial_transactions.repo;

import com.financial.tracker.financial_transactions.model.SplitwiseSyncRun;
import org.springframework.data.repository.ListCrudRepository;
import java.util.Optional;

public interface SplitwiseSyncRunRepository extends ListCrudRepository<SplitwiseSyncRun, Long> {

    Optional<SplitwiseSyncRun> findFirstByUserIdOrderByStartedAtDesc(Long userId);
}
