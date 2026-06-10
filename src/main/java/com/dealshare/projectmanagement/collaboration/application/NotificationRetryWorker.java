package com.dealshare.projectmanagement.collaboration.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "workers.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRetryWorker {

    private final CollaborationService collaboration;

    public NotificationRetryWorker(CollaborationService collaboration) {
        this.collaboration = collaboration;
    }

    @Scheduled(fixedDelayString = "${notifications.retry.fixed-delay-ms:10000}")
    public void retryDueNotifications() {
        collaboration.retryDueNotifications();
    }
}
