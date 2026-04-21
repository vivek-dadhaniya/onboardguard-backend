package com.onboardguard.shared.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * CORE ENGINE: Processes the Thymeleaf HTML and sends the email.
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

//            String htmlContent = templateEngine.process("email/templates/" + templateName, context);

            // Spring Boot automatically prepends "classpath:/templates/"
            // and appends ".html" to whatever string you pass here!
            String htmlContent = templateEngine.process("email/" + templateName, context);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Email successfully sent to {} using template [{}]", to, templateName);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}. Reason: {}", to, e.getMessage());
        }
    }
}
