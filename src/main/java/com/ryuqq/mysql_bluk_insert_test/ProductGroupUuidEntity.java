package com.ryuqq.mysql_bluk_insert_test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * UUIDv7 PK 를 사용하는 ProductGroup 엔티티.
 *
 * <p>{@link ProductGroupEntity} (BIGINT AUTO_INCREMENT) 와 대비되는 비교군.
 * PK 가 앱 레벨에서 미리 생성되므로 Hibernate 의 IDENTITY 한계
 * (단건 INSERT 강제) 가 적용되지 않아 batch insert 가 가능하다.
 *
 * <p>매핑: {@code UUID} → {@code BINARY(16)}.
 * 기본값(char(36)) 대비 절반 크기 + 인덱스 효율 ↑.
 * 자세한 트레이드오프는 ADR-0002 참조.
 *
 * <p><strong>{@link Persistable} 구현 이유:</strong> Spring Data JPA 의 {@code saveAll(...)} 은
 * 엔티티가 "새 것"인지 판단할 때 기본적으로 ID 가 null 인지 확인한다.
 * UUIDv7 처럼 앱 레벨에서 PK 를 미리 생성하면 ID 가 null 이 아니므로 EntityManager 가
 * {@code merge} 경로 (= SELECT 후 INSERT/UPDATE) 로 가버려 batch insert 의 이득이 사라진다.
 * {@code Persistable.isNew()} 를 직접 노출하면 Hibernate 는 {@code persist} (= INSERT only) 를 사용한다.
 */
@Entity
@Table(name = "PRODUCT_GROUP_UUID_TEST")
public class ProductGroupUuidEntity implements Persistable<UUID> {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "ID", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "PRODUCT_GROUP_NAME", nullable = false, length = 150)
    private String productGroupName;

    @Transient
    private boolean isNew = true;

    protected ProductGroupUuidEntity() {
    }

    public ProductGroupUuidEntity(UUID id, String productGroupName) {
        this.id = id;
        this.productGroupName = productGroupName;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getProductGroupName() {
        return productGroupName;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
