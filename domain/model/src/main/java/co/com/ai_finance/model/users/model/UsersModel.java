package co.com.ai_finance.model.users.model;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
//import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UsersModel {
    private UUID id;
    private String email;
    private String username;
    private String names;
    private String lastnames;
    private String password;
    private Double monthlyIncome;
    private String created_by;
    private String updated_by;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
