package co.com.ai_finance.model.users.gateways;

import co.com.ai_finance.model.users.model.UsersModel;
import reactor.core.publisher.Mono;

public interface UsersRepositoryPort {
    Mono<Boolean> validateUserByEmail(String email);
    Mono<Boolean> validateUserByUsername(String username);
    Mono<UsersModel> create(UsersModel usersModel);
}
