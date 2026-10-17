package co.com.ai_finance.api.controllers.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static co.com.ai_finance.model.common.enums.SuccessMessages.SUCCESS_MESSAGE_HEALTH;

@Component
public class HealthHandler {

    public Mono<ServerResponse> health(ServerRequest request){
        return ServerResponse.ok().bodyValue(SUCCESS_MESSAGE_HEALTH.getMessage());
    }
}
