package co.com.ai_finance.api.response;

import java.util.UUID;

public record UsersResponse(
        UUID id,
        String email,
        String lastnames,
        String names,
        Double monthlyIncome
) {
}
