package com.dealshare.projectmanagement.collaboration.application;

import com.dealshare.projectmanagement.infrastructure.persistence.entity.NotificationEntity;

public interface NotificationDeliveryPort {

    void deliver(NotificationEntity notification);
}
