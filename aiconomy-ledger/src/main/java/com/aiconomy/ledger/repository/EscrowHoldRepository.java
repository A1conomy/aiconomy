package com.aiconomy.ledger.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiconomy.ledger.domain.EscrowHold;
import com.aiconomy.ledger.domain.EscrowStatus;

public interface EscrowHoldRepository extends JpaRepository<EscrowHold, UUID> {

	Optional<EscrowHold> findByTaskIdAndStatus(UUID taskId, EscrowStatus status);

}
