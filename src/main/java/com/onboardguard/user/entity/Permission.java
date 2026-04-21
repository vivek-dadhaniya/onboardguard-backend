package com.onboardguard.user.entity;

import com.onboardguard.shared.common.enums.PermissionCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_code", length = 100, unique = true, nullable = false)
    private PermissionCode permissionCode;

    @Column(name = "permission_name", length = 150, nullable = false)
    private String permissionName;

    @Column(name = "module", length = 50, nullable = false)
    private String module;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
