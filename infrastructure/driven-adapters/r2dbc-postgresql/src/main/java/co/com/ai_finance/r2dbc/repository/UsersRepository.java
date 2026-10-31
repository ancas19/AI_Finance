package co.com.ai_finance.r2dbc.repository;

import co.com.ai_finance.r2dbc.entities.UsersEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UsersRepository extends ReactiveCrudRepository<UsersEntity, UUID>, ReactiveQueryByExampleExecutor<UsersEntity> {
    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByUsername(String username);
}
