package de.uniks.vaadin.crm.security;

import de.uniks.postgres.db.model.VerificationUser;
import de.uniks.postgres.db.utils.UserVerificationPostgreSql;
import de.uniks.vaadin.crm.security.model.CustomUserDetails;
import lombok.SneakyThrows;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class UserVerificationService implements UserDetailsService {
    private static final Logger LOG = Logger.getLogger(UserVerificationService.class.getName());

    @SneakyThrows
    @Override
    public UserDetails loadUserByUsername(String uuid) throws UsernameNotFoundException {
        // retrieve login data for username from db and create CustomUserDetails to hand it to spring security handler
        UserVerificationPostgreSql verificationDb = new UserVerificationPostgreSql();

        String timestamp = LocalDateTime.now().toString(); // TODO: maybe add a unique id to login entry
        Optional<Integer> loginVerificationEntry = verificationDb.createLoginVerificationEntry(uuid, timestamp);

        if (loginVerificationEntry.isEmpty() || loginVerificationEntry.get() != 1) {
            // entries should always be able to insert in db, but not if there is already a valid entry
            LOG.log(Level.WARNING, "DBInterface wasn't able to add a validation entry to DB to validate and login with!\n" +
                    "Seems like there is someone trying to login/validate multiple times.");
            throw new UsernameNotFoundException("Try again in >10 min, login is blocked!");
        }

        LocalDateTime timeoutTime = LocalDateTime.now().plus(10, ChronoUnit.MINUTES);

        while (timeoutTime.compareTo(LocalDateTime.now()) > 0) {
            TimeUnit.SECONDS.sleep(10);
            Optional<VerificationUser> verificationUser = verificationDb.checkForLoginVerification(uuid, timestamp);
            if (verificationUser.isPresent()) {
                VerificationUser user = verificationUser.get();
                CustomUserDetails customUserDetails = new CustomUserDetails(user.getUuid(), user.getPin());
                return customUserDetails;
            }
        }
        throw new UsernameNotFoundException("Timeout");
    }
}
