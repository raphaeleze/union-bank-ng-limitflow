package com.limitflow.backend.domain.user;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * The {@code id} is client-assigned (not DB-generated), so Spring Data R2DBC can't tell new
 * from existing rows just by checking for a null id — it would otherwise emit an UPDATE for a
 * brand-new, never-persisted entity, silently affecting zero rows instead of inserting.
 * Implementing {@link Persistable} with an explicit {@code isNew} flag fixes that: the business
 * constructor leaves it {@code true}, while the {@link PersistenceCreator} constructor Spring
 * Data uses to rehydrate rows read back from the database sets it {@code false}.
 */
@Table("users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    private String email;

    @Column("password_hash")
    private String passwordHash;

    private Role role;

    private String phone;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Transient
    private boolean isNew = true;

    public User(String firstName, String lastName, String email, String passwordHash, Role role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    @PersistenceCreator
    User(UUID id, String firstName, String lastName, String email, String passwordHash, Role role, String phone,
            Instant createdAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.phone = phone;
        this.createdAt = createdAt;
        this.isNew = false;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }
}
