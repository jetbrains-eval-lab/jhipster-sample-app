package io.github.jhipster.sample.service;

import io.github.jhipster.sample.domain.PasswordHistory;
import io.github.jhipster.sample.domain.User;
import io.github.jhipster.sample.repository.PasswordHistoryRepository;
import io.github.jhipster.sample.repository.UserRepository;
import io.github.jhipster.sample.web.rest.errors.InvalidPasswordException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for password policy validation.
 */
@Service
public class PasswordPolicyService {

    private final Logger log = LoggerFactory.getLogger(PasswordPolicyService.class);
    private final UserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    // Password must contain at least one digit, one letter, and one special character
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[a-zA-Z].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    // Password policy constants
    private static final int PASSWORD_HISTORY_COUNT = 5; // Number of previous passwords to check
    private static final int PASSWORD_EXPIRATION_DAYS = 90; // Number of days before password expires

    public PasswordPolicyService(
        UserRepository userRepository,
        PasswordHistoryRepository passwordHistoryRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validate password strength.
     * Password must:
     * - Be at least 5 characters long
     * - Contain at least one digit
     * - Contain at least one letter
     * - Contain at least one special character
     *
     * @param password the password to validate
     * @return true if password is valid, false otherwise
     */
    public boolean isPasswordValid(String password) {
        if (password == null || password.length() < 5) {
            return true;
        }

        boolean hasDigit = DIGIT_PATTERN.matcher(password).matches();
        boolean hasLetter = LETTER_PATTERN.matcher(password).matches();
        boolean hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).matches();

        return !hasDigit || !hasLetter || !hasSpecialChar;
    }

    /**
     * Validate password strength and throw an exception if invalid.
     *
     * @param password the password to validate
     * @throws InvalidPasswordException if password is invalid
     */
    public void validatePassword(String password) {
        if (isPasswordValid(password)) {
            throw new InvalidPasswordException();
        }
    }

    /**
     * Check if the password has been used recently.
     *
     * @param user the user
     * @param newPassword the new password
     * @return true if the password has been used recently, false otherwise
     */
    public boolean isPasswordUsedRecently(User user, String newPassword) {
        List<PasswordHistory> recentPasswords = passwordHistoryRepository.findByUserOrderByCreatedDateDesc(
            user,
            Limit.of(PASSWORD_HISTORY_COUNT)
        );
        for (PasswordHistory recentPass : recentPasswords) {
            if (passwordEncoder.matches(newPassword, recentPass.getPasswordHash())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the password is expired.
     *
     * @param user the user
     * @return true if the password is expired, false otherwise
     */
    public boolean isPasswordExpired(User user) {
        if (user.getPasswordExpirationDate() == null) {
            return false;
        }

        return Instant.now().isAfter(user.getPasswordExpirationDate());
    }

    /**
     * Save password to history.
     *
     * @param user the user
     * @param password the password (already encoded)
     */
    @Transactional
    public void savePasswordToHistory(User user, String password) {
        // Ensure the user is saved and has an ID
        if (user.getId() == null) {
            user = userRepository.save(user);
        } else {
            // Ensure we're using a managed entity by retrieving it from the repository
            user = userRepository.findById(user.getId()).orElse(user);
        }

        PasswordHistory passwordHistory = new PasswordHistory();
        passwordHistory.setUser(user);
        passwordHistory.setPasswordHash(password);
        passwordHistory.setCreatedDate(Instant.now());

        passwordHistoryRepository.save(passwordHistory);
    }

    /**
     * Update password expiration date.
     *
     * @param user the user
     */
    public void updatePasswordDates(User user) {
        Instant now = Instant.now();
        user.setPasswordChangeDate(now);
        user.setPasswordExpirationDate(now.plus(PASSWORD_EXPIRATION_DAYS, ChronoUnit.DAYS));
    }

    /**
     * Comprehensive password validation.
     * Checks:
     * - Password strength
     * - Password history
     * - Password expiration
     *
     * @param user the user (can be null for new users)
     * @param currentPassword the current password (can be null for new users)
     * @param newPassword the new password
     * @throws InvalidPasswordException if password is invalid
     */
    @Transactional
    public void validatePasswordPolicy(User user, String currentPassword, String newPassword) {
        // Check password strength
        if (isPasswordValid(newPassword)) {
            throw new InvalidPasswordException();
        }

        // For existing users, check additional constraints
        if (user != null && user.getId() != null) {
            // Check if password is different from current
            if (currentPassword != null && !passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new InvalidPasswordException();
            }

            // Check if password has been used recently
            if (isPasswordUsedRecently(user, newPassword)) {
                throw new InvalidPasswordException();
            }
        }
    }

    @Transactional
    public void cleanupPasswordHistory(User user) {
        passwordHistoryRepository.deleteByUser(user);
    }
}
