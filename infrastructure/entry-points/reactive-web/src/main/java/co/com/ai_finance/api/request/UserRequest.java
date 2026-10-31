package co.com.ai_finance.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserRequest(
        @NotNull
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        String email,
        @NotNull
        @Pattern(regexp = "^[a-zA-Z0-9 ]$")
        String lastnames,
        @NotNull
        @Pattern(regexp = "^[a-zA-Z0-9 ]$")
        String names,
        @NotNull
        String password,
        @NotNull
        @Min(value = 0)
        Double monthlyIncome
) {
}
