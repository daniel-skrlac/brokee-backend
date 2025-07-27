package model.notification;

import jakarta.validation.constraints.NotBlank;


public record RegisterPushDTO(
        @NotBlank
        String playerId
) {
}
