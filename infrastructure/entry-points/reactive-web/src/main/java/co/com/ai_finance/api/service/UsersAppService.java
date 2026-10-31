package co.com.ai_finance.api.service;

import co.com.ai_finance.api.response.UsersResponse;
import co.com.ai_finance.model.users.model.UsersModel;
import co.com.ai_finance.usecase.users.UsersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UsersAppService {
    private final UsersUseCase usersUseCase;
    private final TransactionalOperator transactionalOperator;

    public Mono<UsersResponse> createUsers(UsersModel usersModel){
        return usersUseCase.createUser(usersModel)
                .map(PropertyMapper.Source.Always.Mapper::toUsersResponse)
                .as(transactionalOperator::transactional);
    }
}
