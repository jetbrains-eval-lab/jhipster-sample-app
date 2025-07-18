package io.github.jhipster.sample.repository;

import io.github.jhipster.sample.domain.PasswordHistory;
import io.github.jhipster.sample.domain.User;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PasswordHistory entity.
 */
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    List<PasswordHistory> findByUserOrderByCreatedDateDesc(User user, Limit limit);

    /**
     * Find password histories by user and created date after the given date.
     *
     * @param user the user
     * @param date the date
     * @return the list of password histories
     */
    List<PasswordHistory> findByUserAndCreatedDateAfter(User user, Instant date);

    /**
     * Delete password histories by user.
     *
     * @param user the user
     */
    void deleteByUser(User user);
}
