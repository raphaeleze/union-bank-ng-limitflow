package com.limitflow.backend.application.notification;

import com.limitflow.backend.domain.push.PushToken;
import com.limitflow.backend.domain.push.PushTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Test
    void pushSendsToEveryRegisteredTokenForTheUser() {
        UUID userId = UUID.randomUUID();
        PushToken token = new PushToken(userId, "ExponentPushToken[abc]", "ios");
        when(pushTokenRepository.findByUserId(userId)).thenReturn(Flux.just(token));
        RecordingExchangeFunction exchangeFunction = new RecordingExchangeFunction();
        PushNotificationService service = newService(exchangeFunction);

        StepVerifier.create(service.push(userId, "Title", "Body")).verifyComplete();

        assertThat(exchangeFunction.requestCount()).isEqualTo(1);
    }

    @Test
    void pushIsANoOpWhenTheUserHasNoRegisteredToken() {
        UUID userId = UUID.randomUUID();
        when(pushTokenRepository.findByUserId(userId)).thenReturn(Flux.empty());
        RecordingExchangeFunction exchangeFunction = new RecordingExchangeFunction();
        PushNotificationService service = newService(exchangeFunction);

        StepVerifier.create(service.push(userId, "Title", "Body")).verifyComplete();

        assertThat(exchangeFunction.requestCount()).isZero();
    }

    private PushNotificationService newService(ExchangeFunction exchangeFunction) {
        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        return new PushNotificationService(pushTokenRepository, webClient);
    }

    /** A hand-rolled {@link ExchangeFunction} test double — real HTTP mocking libraries
     * aren't a dependency of this project and would be a disproportionate addition for one
     * test class. */
    private static class RecordingExchangeFunction implements ExchangeFunction {

        private int requests = 0;

        @Override
        public Mono<ClientResponse> exchange(ClientRequest request) {
            requests++;
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        }

        int requestCount() {
            return requests;
        }
    }
}
