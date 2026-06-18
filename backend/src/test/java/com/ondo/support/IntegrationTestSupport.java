package com.ondo.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합테스트 베이스. MySQL 8 컨테이너를 싱글턴으로 한 번 띄우고 @ServiceConnection 으로 DataSource 를 주입한다.
 * Flyway V1 이 컨테이너에 적용된다(스키마 정본 검증).
 *
 * 싱글턴 패턴: static 블록에서 직접 start() 한다. @Testcontainers 로 관리하면 첫 테스트 클래스 종료 시
 * 컨테이너가 멈추는데, 캐시된 스프링 컨텍스트는 그 DataSource 를 계속 참조해 이후 클래스가 실패한다.
 * 컨테이너는 JVM 종료 시 Ryuk 가 정리한다.
 */
@SpringBootTest
public abstract class IntegrationTestSupport {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));

    static {
        MYSQL.start();
    }
}
