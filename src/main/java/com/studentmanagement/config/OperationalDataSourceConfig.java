package com.studentmanagement.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 운영(OLTP) 데이터소스 설정 — 기본(@Primary) datasource.
 *
 * 학습 분석(EP-08)에서 분석용 datasource를 추가하면서, Spring Boot의 단일 datasource
 * 자동구성을 멀티 datasource로 전환한다. 운영 측은 명시적 @Primary로 지정하여
 * 기존 전 기능(레포지토리·@Transactional)이 그대로 동작하도록 보존한다.
 *
 * - 엔티티 스캔: com.studentmanagement.domain (운영 엔티티)
 * - 레포지토리: com.studentmanagement.repository
 * - ddl-auto/dialect는 spring.jpa.* 프로파일 설정을 그대로 사용(EntityManagerFactoryBuilder).
 *   따라서 dev=create-drop / prod=update 동작이 유지된다.
 *
 * 슬라이스 테스트(@DataJpaTest)에서는 비활성화(@Profile("!test"))하여 Boot 기본 단일
 * datasource 자동구성을 사용한다 — 멀티 datasource가 슬라이스의 스키마 생성을 방해하지 않도록.
 */
@Configuration
@Profile("!test")
@EnableJpaRepositories(
        basePackages = "com.studentmanagement.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class OperationalDataSourceConfig {

    // 멀티 datasource로 커스텀 EMF를 만들면 Boot가 spring.jpa.hibernate.ddl-auto를
    // 빌더 기본값으로 자동 주입하지 않는다(운영 스키마 미생성 버그). 명시적으로 주입한다.
    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String ddlAuto;

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataSource") DataSource dataSource) {
        Map<String, Object> props = new HashMap<>();
        // 프로파일별 ddl-auto(dev=create-drop / prod=update / 기본=validate)를 명시 적용
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        return builder
                .dataSource(dataSource)
                .packages("com.studentmanagement.domain")
                .persistenceUnit("operational")
                .properties(props)
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
