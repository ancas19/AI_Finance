package co.com.ai_finance.r2dbc.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@Table(name="users")
@NoArgsConstructor
@AllArgsConstructor
public class UsersEntity extends AbstractAudithEntity {
    @Id
    @Column("id")
    private UUID id;

    @Column("email")
    private String email;

    @Column("username")
    private String username;

    @Column("names")
    private String names;

    @Column("lastnames")
    private String lastnames;

    @Column("password")
    private String password;

    @Column("monthly_income")
    private Double monthlyIncome;

}
