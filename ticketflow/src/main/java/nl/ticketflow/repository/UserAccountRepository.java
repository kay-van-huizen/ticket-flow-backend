package nl.ticketflow.repository;

import nl.ticketflow.entity.UserAccount;
import nl.ticketflow.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    List<UserAccount> findByRole(UserRole role);
}
