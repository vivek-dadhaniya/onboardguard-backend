package com.onboardguard.shared.email.listener;

import com.onboardguard.shared.common.events.*;
import com.onboardguard.shared.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final EmailService emailService;

    // Inject your system email directly from the application.yml
    @Value("${spring.mail.username:}")
    private String adminEmail;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCandidateRegistered(CandidateRegisteredEvent event) {
        log.info("Event Received: Sending Welcome Email to {}", event.candidateEmail());

        Context context = new Context();
        context.setVariable("name", event.candidateName());
        context.setVariable("portalUrl", "https://onboardguard.com/login");

        emailService.sendHtmlEmail(event.candidateEmail(), "Welcome to OnboardGuard Registration", "welcome", context);
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAlertGenerated(AlertGeneratedEvent event) {
        log.info("Event Received: Sending Alert Notification for Case {}", event.alertId());

        Context context = new Context();
        context.setVariable("alertId", event.alertId());
        context.setVariable("candidateName", event.candidateName());
        context.setVariable("severity", event.severity());

        emailService.sendHtmlEmail(event.officerEmail(), "URGENT: New " + event.severity() + " Alert Generated", "alert-notification", context);
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCaseResolved(CaseResolvedEvent event) {
        log.info("Event Received: Sending Case Resolution to {}", event.candidateEmail());

        Context context = new Context();
        context.setVariable("candidateName", event.candidateName());
        context.setVariable("reason", event.reason());

        String template = event.isCleared() ? "case-resolved-cleared" : "case-resolved-rejected";
        String subject = event.isCleared() ? "Background Verification Cleared" : "Important Update Regarding Your Application";

        emailService.sendHtmlEmail(event.candidateEmail(), subject, template, context);
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentRejected(DocumentRejectedEvent event) {
        log.info("Event Received: Sending Document Rejection to {}", event.candidateEmail());

        Context context = new Context();
        context.setVariable("candidateName", event.candidateName());
        context.setVariable("documentType", event.documentType());
        context.setVariable("reason", event.reason());
        context.setVariable("uploadUrl", "https://onboardguard.com/candidate/documents");

        emailService.sendHtmlEmail(event.candidateEmail(), "Action Required: Re-upload " + event.documentType(), "doc-rejection", context);
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOfficerCreated(OfficerCreatedEvent event) {
        log.info("Event Received: Sending temp password email to officer {}", event.officerEmail());

        Context context = new Context();
        context.setVariable("name",          event.officerName());
        context.setVariable("email",         event.officerEmail());
        context.setVariable("tempPassword",  event.plainPassword());
        context.setVariable("role",          event.role());
        context.setVariable("createdBy",     event.createdByEmail());
        context.setVariable("loginUrl",      "https://onboardguard.com/staff/login");

        emailService.sendHtmlEmail(
                event.officerEmail(),
                "Your OnboardGuard Officer Account Credentials",
                "officer-welcome",
                context
        );
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSlaBreach(SlaBreachNotificationEvent event) {
        log.info("Event Received: Sending SLA Breach Notification to Management at {}", event.timestamp());

        Context context = new Context();
        context.setVariable("newBreachedAlerts", event.newBreachedAlerts());
        context.setVariable("newBreachedCases", event.newBreachedCases());

        // Format the instant into a readable string
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                .ofPattern("MMM dd, yyyy - HH:mm:ss z")
                .withZone(java.time.ZoneId.of("UTC"));
        context.setVariable("timestamp", formatter.format(event.timestamp()));
        context.setVariable("dashboardUrl", "https://onboardguard.com/admin/dashboard");

        emailService.sendHtmlEmail(
                adminEmail,
                "URGENT: Operations SLA Breach Detected",
                "sla-breach-notification",
                context
        );
    }
}