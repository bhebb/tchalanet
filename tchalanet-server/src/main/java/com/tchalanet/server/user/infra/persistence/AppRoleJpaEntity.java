package com.tchalanet.server.user.infra.persistence;

import com.tchalanet.server.common.infra.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "app_role")
@Getter
@Setter
@NoArgsConstructor
@Audited
public class AppRoleJpaEntity extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "description")
  private String description;

  @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
  private Set<AppUserJpaEntity> users = new HashSet<>();
}
