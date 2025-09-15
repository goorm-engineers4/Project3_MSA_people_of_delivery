package com.example.cloudfour.authservice.domain.auth.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl 단위 테스트")
class EmailServiceImplTest {

    @Mock JavaMailSender mailSender;

    @Test
    @DisplayName("정상 메일 전송")
    void send_ok() throws Exception {
        var svc = new EmailServiceImpl(mailSender);
        MimeMessage msg = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(msg);

        svc.sendSimpleMessage("to@x.com", "제목", "<b>본문</b>");

        verify(mailSender).send(msg);
    }

    @Test
    @DisplayName("전송 실패 시 런타임 예외 래핑")
    void send_fail_wraps_exception() throws Exception {
        var svc = new EmailServiceImpl(mailSender);
        MimeMessage msg = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(msg);
        doThrow(new RuntimeException("fail")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> svc.sendSimpleMessage("to@x.com", "제목", "본문"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to send email");
    }
}
