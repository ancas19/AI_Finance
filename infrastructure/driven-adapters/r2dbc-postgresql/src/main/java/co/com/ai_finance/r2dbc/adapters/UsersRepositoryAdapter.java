package co.com.ai_finance.r2dbc.adapters;


import co.com.ai_finance.model.users.gateways.UsersRepositoryPort;
import co.com.ai_finance.model.users.model.UsersModel;
import co.com.ai_finance.r2dbc.entities.UsersEntity;
import co.com.ai_finance.r2dbc.helper.ReactiveAdapterOperations;
import co.com.ai_finance.r2dbc.repository.UsersRepository;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class UsersRepositoryAdapter extends ReactiveAdapterOperations<UsersModel, UsersEntity, UUID, UsersRepository> implements UsersRepositoryPort {

    public UsersRepositoryAdapter(UsersRepository usersRepository,ObjectMapper objectMapper){
        super(usersRepository,objectMapper, d-> objectMapper.map(d,UsersModel.class));
    }
    @Override
    public Mono<Boolean> validateUserByEmail(String email) {
        return this.repository.existsByEmail(email);
    }

    @Override
    public Mono<Boolean> validateUserByUsername(String username) {
        return this.repository.existsByUsername(username);
    }

    @Override
    public Mono<UsersModel> create(UsersModel usersModel) {
        return this.repository.save(this.mapper.map(usersModel, UsersEntity.class))
                .map(userCreated->this.mapper.map(userCreated,UsersModel.class));
    }
}
