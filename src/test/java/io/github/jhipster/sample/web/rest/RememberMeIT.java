package io.github.jhipster.sample.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jhipster.sample.IntegrationTest;
import io.github.jhipster.sample.domain.Authority;
import io.github.jhipster.sample.domain.User;
import io.github.jhipster.sample.repository.UserRepository;
import io.github.jhipster.sample.security.AuthoritiesConstants;
import io.github.jhipster.sample.web.rest.vm.LoginVM;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for the remember me functionality.
 */
@AutoConfigureMockMvc
@IntegrationTest
class RememberMeIT {

    static final String TEST_USER_LOGIN = "test-user";
    static final String TEST_USER_PASSWORD = "test-password";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    private User user;

    @BeforeEach
    public void setup() {
        // Create a test user if it doesn't exist
        Optional<User> userOptional = userRepository.findOneByLogin(TEST_USER_LOGIN);
        if (userOptional.isEmpty()) {
            user = new User();
            user.setLogin(TEST_USER_LOGIN);
            user.setPassword(passwordEncoder.encode(TEST_USER_PASSWORD));
            Set<Authority> authorities = new HashSet<>();
            authorities.add(new Authority().name(AuthoritiesConstants.ADMIN));
            user.setAuthorities(authorities);
            user.setActivated(true);
            userRepository.save(user);
        } else {
            user = userOptional.get();
        }
    }

    @Test
    void testRememberMe() throws Exception {
        // Login with remember me
        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN);
        login.setPassword(TEST_USER_PASSWORD);
        login.setRememberMe(true);

        // Perform login and get the response cookies
        MvcResult loginResult = mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(convertObjectToJsonBytes(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").exists())
            .andReturn();

        // Extract the remember-me cookie
        Cookie rememberMeCookie = null;
        for (Cookie cookie : loginResult.getResponse().getCookies()) {
            if ("remember-me".equals(cookie.getName())) {
                rememberMeCookie = cookie;
                break;
            }
        }

        // Verify that a remember-me cookie was created
        assertThat(rememberMeCookie).isNotNull();
        assertThat(rememberMeCookie.getValue()).isNotEmpty();

        // Test authentication with the remember me cookie
        mockMvc.perform(get("/api/admin/users").cookie(rememberMeCookie)).andExpect(status().isOk()).andReturn();
    }

    @Test
    void testRememberMeWithInvalidToken() throws Exception {
        // Login with remember me to get a valid cookie first
        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN);
        login.setPassword(TEST_USER_PASSWORD);
        login.setRememberMe(true);

        MvcResult loginResult = mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(convertObjectToJsonBytes(login)))
            .andExpect(status().isOk())
            .andReturn();

        // Extract the remember-me cookie
        Cookie rememberMeCookie = null;
        for (Cookie cookie : loginResult.getResponse().getCookies()) {
            if ("remember-me".equals(cookie.getName())) {
                rememberMeCookie = cookie;
                break;
            }
        }

        assertThat(rememberMeCookie).isNotNull();

        // Create a cookie with an invalid token value
        String[] parts = rememberMeCookie.getValue().split(":");
        String cookieValue = parts[0] + ":invalid-token-value";
        Cookie invalidCookie = new Cookie("remember-me", cookieValue);

        // Test authentication with the invalid remember me cookie
        mockMvc.perform(get("/api/admin/users").cookie(invalidCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void testRememberMeWithMalformedToken() throws Exception {
        // Create a cookie with a malformed token
        Cookie malformedCookie = new Cookie("remember-me", "malformed-token");

        // Test authentication with the malformed remember me cookie
        mockMvc.perform(get("/api/admin/users").cookie(malformedCookie)).andExpect(status().isUnauthorized());
    }

    /**
     * Convert an object to JSON byte array.
     *
     * @param object the object to convert.
     * @return the JSON byte array.
     * @throws IOException if the object cannot be converted to JSON.
     */
    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsBytes(object);
    }
}
