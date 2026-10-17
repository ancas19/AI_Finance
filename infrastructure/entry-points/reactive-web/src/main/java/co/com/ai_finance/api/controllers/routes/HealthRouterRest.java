package co.com.ai_finance.api.controllers.routes;

import co.com.ai_finance.api.controllers.handlers.HealthHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class HealthRouterRest {
    private final HealthHandler healthHandler;

    public HealthRouterRest(HealthHandler healthHandler) {
        this.healthHandler = healthHandler;
    }

    @Bean
    public RouterFunction<ServerResponse> healthRouter() {
        return RouterFunctions.route()
                .GET("/health",healthHandler::health)
                .build();
    }
}
