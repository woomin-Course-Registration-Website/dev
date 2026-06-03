package com.studentmanagement.dto.user;

import com.studentmanagement.domain.User;
import lombok.Getter;

@Getter
public class NotificationSettingsResponse {
    private final boolean notifyGrade;
    private final boolean notifyFeedback;
    private final boolean notifyCounseling;

    public NotificationSettingsResponse(User user) {
        this.notifyGrade = user.isNotifyGrade();
        this.notifyFeedback = user.isNotifyFeedback();
        this.notifyCounseling = user.isNotifyCounseling();
    }
}
