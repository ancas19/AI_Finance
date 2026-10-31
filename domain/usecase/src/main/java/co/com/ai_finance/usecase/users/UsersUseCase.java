package co.com.ai_finance.usecase.users;

import co.com.ai_finance.model.common.exceptions.BadRequestException;
import co.com.ai_finance.model.users.gateways.UsersRepositoryPort;
import co.com.ai_finance.model.users.model.UsersModel;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static co.com.ai_finance.model.common.enums.ErrorMessages.ERROR_MESSSAGE_EMAIL_ALREADY_EXISTS;


@RequiredArgsConstructor
public class UsersUseCase {
    private final UsersRepositoryPort usersRepositoryPort;

    public Mono<UsersModel> createUser(UsersModel usersModel) {
        return usersRepositoryPort.validateUserByEmail(usersModel.getEmail())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new BadRequestException(ERROR_MESSSAGE_EMAIL_ALREADY_EXISTS.getMessage())))
                .flatMap(ignored -> createUsername(usersModel.getNames(), usersModel.getLastnames()))
                .flatMap(userame -> {
                            usersModel.setUsername(userame);
                            return this.usersRepositoryPort.create(usersModel);
                        }
                );
    }

    private Mono<String> createUsername(String names, String lastnames) {
        String username = names.split("\\s+")[0].trim().toUpperCase() + lastnames.split("\\s+")[0].trim().toUpperCase();
        return findAvailableUsername(username, 0);
    }

    private Mono<String> findAvailableUsername(String baseUsername, int counter) {
        String candidate = counter == 0 ? baseUsername : baseUsername + counter;
        return usersRepositoryPort.validateUserByUsername(candidate)
                .filter(exists -> !exists)
                .map(exists -> candidate)
                .switchIfEmpty(
                        Mono.defer(() -> findAvailableUsername(baseUsername, counter + 1))
                );
    }
}
