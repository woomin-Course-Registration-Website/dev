package com.studentmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 메일 발송 서비스
 *
 * AuthService에서 분리하여 별도 빈으로 두는 이유:
 *  - @Async가 자기-호출(self-invocation) 시 동작하지 않으므로 외부 빈이 필요.
 *  - 메일 발송이 트랜잭션 커밋 이후, 별도 스레드에서 실행되도록 보장.
 *
 * 메일 발송 실패는 로그만 남기고 호출 측에 예외를 전파하지 않습니다.
 * (요청 처리는 이미 완료되었고, 사용자에게 재시도 안내는 별도 UX로 처리)
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("emailExecutor")
    public void sendResetPasswordEmail(String to, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[학생관리시스템] 임시 비밀번호 안내");
            message.setText("임시 비밀번호: " + tempPassword + "\n로그인 후 비밀번호를 변경해주세요.");
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("비밀번호 재설정 메일 발송 실패: to={}, reason={}", to, ex.getMessage());
        }
    }
}
