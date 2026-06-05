package com.financial.tracker.financial_transactions.repo;

import com.financial.tracker.financial_transactions.model.User;
import org.springframework.data.repository.ListCrudRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends ListCrudRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findBySplitwiseEnabledTrue();
}
