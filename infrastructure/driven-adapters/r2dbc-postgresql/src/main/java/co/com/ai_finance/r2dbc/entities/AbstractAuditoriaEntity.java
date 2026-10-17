package co.com.ai_finance.r2dbc.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AbstractAuditoriaEntity {
    @CreatedBy
    @Column("u_creacion")
    private String usuarioCreacion;

    @LastModifiedBy
    @Column("u_actualizacion")
    private String usuarioActualizacion;

    @CreatedDate
    @Column("f_creacion")
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Column("f_actualizacion")
    private LocalDateTime fechaActualizacion;
}
