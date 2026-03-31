package allmart.inventoryservice.domain;

import allmart.inventoryservice.config.SnowflakeGenerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @SnowflakeGenerated
    private Long id;
}