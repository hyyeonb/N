package dev3.nms.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Value("${email.from:nms-alert@company.com}")
    private String fromAddress;

    @Async
    public void sendHtml(String to, String subject, String htmlBody) {
        if (!emailEnabled) {
            log.debug("Email disabled - skipping: to={}, subject={}", to, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("Email send failed: to={}, subject={}, error={}", to, subject, e.getMessage());
        }
    }

    @Async
    public void sendSimple(String to, String subject, String body) {
        if (!emailEnabled) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(message);
            log.info("Email sent: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("Email send failed: to={}, error={}", to, e.getMessage());
        }
    }

    public boolean isEnabled() {
        return emailEnabled;
    }
}
