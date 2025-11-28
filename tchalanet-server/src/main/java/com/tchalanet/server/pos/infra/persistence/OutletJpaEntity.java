package com.tchalanet.server.pos.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "outlet")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class OutletJpaEntity extends BaseTenantEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "zone")
  private String zone;

  @OneToMany(mappedBy = "outlet", fetch = FetchType.LAZY)
  private List<TerminalJpaEntity> terminals = new ArrayList<>();
}
