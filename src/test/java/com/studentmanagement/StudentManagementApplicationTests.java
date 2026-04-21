package com.studentmanagement;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("로컬 MySQL 연결이 필요합니다. Docker 환경에서 실행하세요.")
class StudentManagementApplicationTests {

    @Test
    void contextLoads() {
    }
}
