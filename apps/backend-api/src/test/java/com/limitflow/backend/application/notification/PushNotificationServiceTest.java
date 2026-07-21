package com.limitflow.backend.application.notification;

import com.limitflow.backend.domain.push.PushToken;
import com.limitflow.backend.domain.push.PushTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        ClientRequest request = exchangeFunction.lastRequest();
        assertThat(request.url().toString()).isEqualTo("https://exp.host/--/api/v2/push/send");
        String body = bodyAsString(request);
        assertThat(body)
                .contains("\"to\":\"ExponentPushToken[abc]\"")
                .contains("\"title\":\"Title\"")
                .contains("\"body\":\"Body\"");
    }

    /** Writes the request's {@link BodyInserter} into a {@link MockClientHttpRequest} so the
     * actual JSON sent over the wire can be asserted on, not just that a request happened. */
    private static String bodyAsString(ClientRequest request) {
        MockClientHttpRequest httpRequest = new MockClientHttpRequest(request.method(), request.url());
        BodyInserter.Context context = new BodyInserter.Context() {
            @Override
            public List<HttpMessageWriter<?>> messageWriters() {
                return ExchangeStrategies.withDefaults().messageWriters();
            }

            @Override
            public Optional<ServerHttpRequest> serverRequest() {
                return Optional.empty();
            }

            @Override
            public Map<String, Object> hints() {
                return Collections.emptyMap();
            }
        };
        request.body().insert(httpRequest, context).block();
        return httpRequest.getBodyAsString().block();
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
        // Same baseUrl as the production expoPushClient bean (WebClientConfig), since
        // PushNotificationService.push() never calls .uri(...) itself.
        WebClient webClient = WebClient.builder()
                .baseUrl("https://exp.host/--/api/v2/push/send")
                .exchangeFunction(exchangeFunction)
                .build();
        return new PushNotificationService(pushTokenRepository, webClient);
    }

    /** A hand-rolled {@link ExchangeFunction} test double — real HTTP mocking libraries
     * aren't a dependency of this project and would be a disproportionate addition for one
     * test class. */
    private static class RecordingExchangeFunction implements ExchangeFunction {

        private int requests = 0;
        private ClientRequest lastRequest;

        @Override
        public Mono<ClientResponse> exchange(ClientRequest request) {
            requests++;
            lastRequest = request;
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        }

        int requestCount() {
            return requests;
        }

        ClientRequest lastRequest() {
            return lastRequest;
        }
    }
}
