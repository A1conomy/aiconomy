package com.aiconomy.ledger.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiconomy.ledger.domain.Account;
import com.aiconomy.ledger.domain.AccountType;

/**
 * Persistence layer for {@link Account} entities.
 * Spring Data generates the SQL implementation at runtime.
 */
public interface AccountRepository extends JpaRepository<Account, UUID> {

	Optional<Account> findByOwnerId(String ownerId);

	Optional<Account> findByOwnerIdAndAccountType(String ownerId, AccountType accountType);

}
