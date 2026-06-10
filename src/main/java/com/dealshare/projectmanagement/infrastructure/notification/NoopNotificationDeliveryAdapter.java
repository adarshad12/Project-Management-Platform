package com.dealshare.projectmanagement.infrastructure.notification;

import com.dealshare.projectmanagement.collaboration.application.NotificationDeliveryPort;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class NoopNotificationDeliveryAdapter implements NotificationDeliveryPort {

    @Override
    public void deliver(NotificationEntity notification) {
        // Prototype adapter. A production adapter would call email, Slack, or another notification service.
    }
}
