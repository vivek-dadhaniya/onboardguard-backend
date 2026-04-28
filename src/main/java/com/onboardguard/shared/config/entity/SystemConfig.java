package com.onboardguard.shared.config.entity;

import com.onboardguard.shared.common.entity.BaseEntity;
import com.onboardguard.shared.common.enums.ConfigType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "system_config")
@Getter
@Setter
@NoArgsConstructor
@Audited
public class SystemConfig extends BaseEntity {

    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "config_type", nullable = false)
    private ConfigType configType;

    @Column(name = "description")
    private String description;

    @Column(name = "is_sensitive", nullable = false)
    private Boolean isSensitive = false;

}
