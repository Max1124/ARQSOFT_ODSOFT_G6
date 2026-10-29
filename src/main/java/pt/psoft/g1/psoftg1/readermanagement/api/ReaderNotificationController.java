package pt.psoft.g1.psoftg1.readermanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ChooseNotificationChannelsRequest;
import pt.psoft.g1.psoftg1.readermanagement.services.NotificationPreferenceService;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

/** Reader notification preferences at runtime (US8, QAS6); the reader changes only their own choice. */
@Tag(name = "Readers", description = "Endpoints to manage readers")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/readers/notification-channels")
class ReaderNotificationController {
    private final NotificationPreferenceService preferenceService;
    private final UserService userService;

    @Operation(summary = "Chooses the enabled notification channels through which the authenticated reader is notified,"
            + " and optionally sets the URL that receives the reader's webhook notifications")
    @RolesAllowed(Role.READER)
    @PutMapping
    public ResponseEntity<NotificationChannelsView> chooseChannels(@RequestBody final ChooseNotificationChannelsRequest request,
                                                                   final Authentication authentication) {
        final User loggedUser = userService.getAuthenticatedUser(authentication);
        final ReaderDetails reader = preferenceService.validateAndStore(loggedUser.getUsername(), request.channels(),
                request.webhookUrl());

        return ResponseEntity.ok()
                .body(new NotificationChannelsView(reader.getNotificationChannels().stream().sorted().toList()));
    }
}
