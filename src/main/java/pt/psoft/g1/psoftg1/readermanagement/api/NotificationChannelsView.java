package pt.psoft.g1.psoftg1.readermanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Notification channels chosen by a reader")
public record NotificationChannelsView(List<String> channels) {}
