package com.limitflow.backend.domain.push;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PushTokenTest {

    @Test
    void businessConstructorMarksTheTokenAsNew() {
        PushToken token = new PushToken(UUID.randomUUID(), "ExponentPushToken[abc]", "ios");

        assertThat(token.isNew()).isTrue();
    }
}
