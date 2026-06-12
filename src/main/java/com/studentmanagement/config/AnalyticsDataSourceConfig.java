package com.studentmanagement.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 분석(OLAP) 데이터소스 설정 — student_analytics 별도 DB.
 *
 * 운영 DB와 동일 MySQL 인스턴스의 별도 데이터베이스에 스타 스키마(Fact/Dimension)를 둔다.
 * Flyway 관리 대상이 아니며, ddl-auto(app.analytics.ddl-auto)로 스키마를 자동 생성/갱신한다.
 *
 * - 엔티티 스캔: com.studentmanagement.analytics.domain (Dim/Fact/EtlCheckpoint)
 * - 레포지토리: com.studentmanagement.analytics.repository
 * - 트랜잭션: analyticsTransactionManager (ETL 쓰기·집계 읽기에 명시적으로 사용)
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.studentmanagement.analytics.repository",
        entityManagerFactoryRef = "analyticsEntityManagerFactory",
        transactionManagerRef = "analyticsTransactionManager"
)
public class AnalyticsDataSourceConfig {

    @Value("${app.analytics.ddl-auto:update}")
    private String ddlAuto;

    @Bean
    @ConfigurationProperties("app.analytics.datasource")
    public DataSourceProperties analyticsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource analyticsDataSource(
            @Qualifier("analyticsDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean analyticsEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("analyticsDataSource") DataSource dataSource) {
        Map<String, Object> props = new HashMap<>();
        // 운영 측 spring.jpa.* 와 독립적으로 분석 스키마 ddl-auto를 적용
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        props.put("hibernate.format_sql", true);
        return builder
                .dataSource(dataSource)
                .packages("com.studentmanagement.analytics.domain")
                .persistenceUnit("analytics")
                .properties(props)
                .build();
    }

    @Bean
    public PlatformTransactionManager analyticsTransactionManager(
            @Qualifier("analyticsEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
