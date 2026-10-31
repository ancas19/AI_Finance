package co.com.ai_finance.api.controllers.handlers;

import co.com.ai_finance.api.request.UserRequest;
import co.com.ai_finance.api.utils.CustomValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UsersHandler {

    private final CustomValidator validator;

    public Mono<ServerResponse> createUser(ServerRequest request){
        return request.bodyToMono(UserRequest.class)
                .flatMap(this.validator::validate)
    }
}
