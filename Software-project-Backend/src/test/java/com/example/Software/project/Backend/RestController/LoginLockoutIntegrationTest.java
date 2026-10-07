package com.example.Software.project.Backend.RestController;

import com.example.Software.project.Backend.Model.User;
import com.example.Software.project.Backend.Repository.UserRepository;
import com.example.Software.project.Backend.Service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:lockout;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "logging.level.org.springframework=WARN"})
@AutoConfigureMockMvc
class LoginLockoutIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserService service;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    String username;
    static final String PASSWORD="LockoutTest123!";

    @BeforeEach void createAccount() {
        username="lock"+UUID.randomUUID().toString().replace("-", "");
        users.saveAndFlush(new User(username,username+"@example.test",encoder.encode(PASSWORD),"lecture"));
    }
    String login(String name,String password,int status) throws Exception {
        var result=mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(json.writeValueAsString(Map.of("userID",name,"password",password))))
            .andExpect(status().is(status)).andReturn();
        var body=json.readTree(result.getResponse().getContentAsString());
        if(status!=200) assertFalse(body.has("token"));
        return body.has("token")?body.get("token").asText():body.get("message").asText();
    }
    User state() {return users.findByUsername(username).orElseThrow();}
    void expireLock() {jdbc.update("update `user` set locked_until=? where User_ID=?",LocalDateTime.now().minusSeconds(1),username);}

    @ParameterizedTest @ValueSource(strings={"lecture","admin","superadmin"})
    void fiveFailuresLockEveryRoleWithoutExtendingDeadline(String role) throws Exception {
        User user=state();user.setUsertype(role);users.saveAndFlush(user);
        String token=login(username,PASSWORD,200);
        LocalDateTime before=LocalDateTime.now();
        for(int i=1;i<=5;i++) {
            login(username,"WrongPassword",401);
            assertEquals(i,state().getFailedLoginAttempts());
            if(i<5)assertNull(state().getLockedUntil());
        }
        LocalDateTime until=state().getLockedUntil();
        assertTrue(until.isAfter(before.plusMinutes(14)));
        assertTrue(until.isBefore(LocalDateTime.now().plusMinutes(16)));
        assertTrue(login(username,PASSWORD,423).contains("temporarily locked"));
        login(username,"WrongAgain",423);
        assertEquals(until,state().getLockedUntil());
        assertEquals(5,state().getFailedLoginAttempts());
        mvc.perform(get("/api/modules/all").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        expireLock();login(username,PASSWORD,200);
        assertEquals(0,state().getFailedLoginAttempts());assertNull(state().getLockedUntil());
    }

    @Test void expiredLockStartsFreshFailureWindow() throws Exception {
        for(int i=0;i<5;i++)service.recordFailedLogin(username);
        expireLock();login(username,"Wrong",401);
        assertEquals(1,state().getFailedLoginAttempts());assertNull(state().getLockedUntil());
        login(username,PASSWORD,200);assertEquals(0,state().getFailedLoginAttempts());
    }

    @Test void successfulLoginResetsConsecutiveFailures() throws Exception {
        for(int i=0;i<4;i++)login(username,"Wrong",401);
        login(username,PASSWORD,200);
        login(username,"Wrong",401);
        assertEquals(1,state().getFailedLoginAttempts());assertNull(state().getLockedUntil());
    }

    @Test void unknownAccountDoesNotCreateStateOrRevealExistence() throws Exception {
        long count=users.count();
        String unknown=login("missing"+username,"Wrong",401);
        assertEquals(unknown,login(username,"Wrong",401));assertEquals(count,users.count());
    }

    @Test void concurrentFailuresAreCountedAndCappedAtFive() throws Exception {
        var pool=Executors.newFixedThreadPool(8);
        try {
            var start=new CountDownLatch(1);var futures=new ArrayList<Future<?>>();
            for(int i=0;i<12;i++)futures.add(pool.submit(()->{start.await();service.recordFailedLogin(username);return null;}));
            start.countDown();for(var future:futures)future.get(20,TimeUnit.SECONDS);
        } finally {pool.shutdownNow();}
        assertEquals(5,state().getFailedLoginAttempts());assertTrue(state().isCurrentlyLocked());
        LocalDateTime until=state().getLockedUntil();service.recordFailedLogin(username);
        assertEquals(until,state().getLockedUntil());
    }

    @Test void lateSuccessfulAuthenticationCannotClearConcurrentLock() {
        for(int i=0;i<5;i++)service.recordFailedLogin(username);
        assertThrows(LockedException.class,()->service.resetFailedLogins(username));
        assertEquals(5,state().getFailedLoginAttempts());assertTrue(state().isCurrentlyLocked());
    }
}
