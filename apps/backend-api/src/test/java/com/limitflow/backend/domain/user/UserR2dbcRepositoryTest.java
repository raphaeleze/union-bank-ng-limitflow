package com.limitflow.backend.domain.user;

import com.limitflow.backend.EmbeddedR2dbcConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_EACH_TEST_METHOD;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY, refresh = BEFORE_EACH_TEST_METHOD)
@Import(EmbeddedR2dbcConfig.class)
class UserR2dbcRepositoryTest {

    @Autowired
    private UserRepository userR2dbcRepository;

    @Test
    void roleEnumRoundTripsAsItsNameString() {
        User user = new User("Test", "User", "role-roundtrip@limitflow.test", "hash", Role.MANAGER);

        StepVerifier.create(
                        userR2dbcRepository.save(user)
                                .flatMap(saved -> userR2dbcRepository.findById(saved.getId())))
                .assertNext(found -> {
                    if (found.getRole() != Role.MANAGER) {
                        throw new AssertionError("Expected role MANAGER, got " + found.getRole());
                    }
                })
                .verifyComplete();
    }
}
