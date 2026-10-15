package com.todoproject.adapter.in.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * Stores the appearance choice on the visitor's device.
 *
 * <p>Strictly functional: it holds one of three enum names and no identifier of any kind, which is
 * why it needs no consent prompt (research R10, security CHK022). {@code HttpOnly} because nothing
 * reads it in the browser — the server renders the theme.
 */
public final class ThemeCookie {

    static final String NAME = "appearance";

    private static final Duration MAX_AGE = Duration.ofDays(365);

    private ThemeCookie() {}

    static AppearancePreference read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return AppearancePreference.SYSTEM;
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())) {
                return AppearancePreference.parse(cookie.getValue());
            }
        }
        return AppearancePreference.SYSTEM;
    }

    static void write(HttpServletResponse response, AppearancePreference preference) {
        ResponseCookie cookie = ResponseCookie.from(NAME, preference.name())
                .path("/")
                .httpOnly(true)
                .secure(false) // local HTTP deployment; revisit with transport security
                .sameSite("Lax")
                .maxAge(MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
