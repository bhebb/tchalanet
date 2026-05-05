package com.tchalanet.server.core.notification.application.policy;

import com.tchalanet.server.core.notification.domain.InvalidNotificationException;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationRecipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationPolicyTest {

    private NotificationPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new NotificationPolicy();
    }

    @Test
    void shouldRejectEmptyRecipients() {
        assertThatThrownBy(() -> policy.validateRecipients(List.of()))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("At least one recipient is required");
    }

    @Test
    void shouldRejectNullRecipients() {
        assertThatThrownBy(() -> policy.validateRecipients(null))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("At least one recipient is required");
    }

    @Test
    void shouldAcceptValidSlackRecipient() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "batch-draws",
            null,
            null
        );

        policy.validateRecipients(List.of(recipient));
        // No exception = success
    }

    @Test
    void shouldRejectSlackWithoutChannelKey() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            null, // missing channelKey
            null,
            null
        );

        assertThatThrownBy(() -> policy.validateRecipients(List.of(recipient)))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("SLACK requires channelKey");
    }

    @Test
    void shouldRejectSlackWithBlankChannelKey() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "  ", // blank channelKey
            null,
            null
        );

        assertThatThrownBy(() -> policy.validateRecipients(List.of(recipient)))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("SLACK requires channelKey");
    }

    @Test
    void shouldAcceptValidEmailRecipient() {
        var recipient = new NotificationRecipient(
            NotificationChannel.EMAIL,
            "user@example.com",
            null,
            null,
            null
        );

        policy.validateRecipients(List.of(recipient));
        // No exception = success
    }

    @Test
    void shouldRejectEmailWithoutTo() {
        var recipient = new NotificationRecipient(
            NotificationChannel.EMAIL,
            null, // missing to
            null,
            null,
            null
        );

        assertThatThrownBy(() -> policy.validateRecipients(List.of(recipient)))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("EMAIL requires to");
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        var recipient = new NotificationRecipient(
            NotificationChannel.EMAIL,
            "not-an-email",
            null,
            null,
            null
        );

        assertThatThrownBy(() -> policy.validateRecipients(List.of(recipient)))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("Invalid email format");
    }

    @Test
    void shouldAcceptValidSmsRecipient() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SMS,
            "+15145551234",
            null,
            null,
            null
        );

        policy.validateRecipients(List.of(recipient));
        // No exception = success
    }

    @Test
    void shouldRejectSmsWithoutTo() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SMS,
            null, // missing to
            null,
            null,
            null
        );

        assertThatThrownBy(() -> policy.validateRecipients(List.of(recipient)))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("SMS requires to");
    }

    @Test
    void shouldRejectSmsWithoutPlusPrefix() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SMS,
            "15145551234", // missing + prefix
            null,
            null,
            null
        );

        assertThatThrownBy(() -> policy.validateRecipients(List.of(recipient)))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("Invalid phone format");
    }

    @Test
    void shouldRejectSmsTooShort() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SMS,
            "+12345", // too short
            null,
            null,
            null
        );

        assertThatThrownBy(() -> policy.validateRecipients(List.of(recipient)))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("Invalid phone format");
    }

    @Test
    void shouldValidateMultipleRecipients() {
        var slackRecipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "batch-draws",
            null,
            null
        );

        var emailRecipient = new NotificationRecipient(
            NotificationChannel.EMAIL,
            "admin@example.com",
            null,
            null,
            null
        );

        policy.validateRecipients(List.of(slackRecipient, emailRecipient));
        // No exception = success
    }

    @Test
    void shouldRejectIfAnyRecipientInvalid() {
        var validRecipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "batch-draws",
            null,
            null
        );

        var invalidRecipient = new NotificationRecipient(
            NotificationChannel.EMAIL,
            "not-an-email",
            null,
            null,
            null
        );

        assertThatThrownBy(() -> policy.validateRecipients(List.of(validRecipient, invalidRecipient)))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("Invalid email format");
    }

    @Test
    void shouldAllowCanSend() {
        assertThat(policy.canSend("ops-test")).isTrue();
        assertThat(policy.canSend("ticket-delivery")).isTrue();
    }
}

