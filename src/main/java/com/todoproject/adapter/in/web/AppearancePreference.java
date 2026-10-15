package com.todoproject.adapter.in.web;

import java.util.Optional;

/**
 * The visitor's appearance choice — three states, not two (FR-019, research R10).
 *
 * <p>The spec originally described a light/dark toggle while its Key Entities allowed
 * follow-device. A two-state control would strand anyone who used it once from ever following
 * their device again, so {@link #SYSTEM} is a first-class state the control cycles back to.
 */
public enum AppearancePreference {

    /** Follow the device. The default, and reachable again after an explicit choice. */
    SYSTEM(null),
    LIGHT("light"),
    DARK("dark");

    private final String dataThemeAttribute;

    AppearancePreference(String dataThemeAttribute) {
        this.dataThemeAttribute = dataThemeAttribute;
    }

    /**
     * The value for {@code <html data-theme>}, or null for {@link #SYSTEM}.
     *
     * <p>Null matters: Thymeleaf omits an attribute whose value is null, so no preference renders no
     * attribute at all and CSS {@code prefers-color-scheme} stays in charge — which is how the
     * device default works with no scripting.
     */
    public String dataThemeAttribute() {
        return dataThemeAttribute;
    }

    /** The next state in the cycle, so one control reaches all three. */
    public AppearancePreference next() {
        return switch (this) {
            case SYSTEM -> LIGHT;
            case LIGHT -> DARK;
            case DARK -> SYSTEM;
        };
    }

    public static AppearancePreference parse(String raw) {
        return parseOptional(raw).orElse(SYSTEM);
    }

    public static Optional<AppearancePreference> parseOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        for (AppearancePreference preference : values()) {
            if (preference.name().equals(raw.trim())) {
                return Optional.of(preference);
            }
        }
        return Optional.empty();
    }
}
