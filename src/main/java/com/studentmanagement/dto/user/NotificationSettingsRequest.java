package com.studentmanagement.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationSettingsRequest {
    private boolean notifyGrade = true;
    private boolean notifyFeedback = true;
    private boolean notifyCounseling = true;
}
