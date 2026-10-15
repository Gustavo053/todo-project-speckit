package com.todoproject.adapter.in.web;

import com.todoproject.adapter.in.web.dto.ThemeForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Sets the appearance preference (FR-019, FR-020, research R10).
 *
 * <p>{@code redirectTo} is validated as an internal path only -- anything else falls back to the
 * board, so this cannot be used as an open redirect.
 */
@Controller
public class ThemeController {

    @PostMapping("/theme")
    String setTheme(
            @Valid @ModelAttribute("themeForm") ThemeForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response) {
        AppearancePreference preference = AppearancePreference.parseOptional(form.getPreference())
                .orElse(AppearancePreference.SYSTEM);
        ThemeCookie.write(response, preference);
        return "redirect:" + safeRedirectTarget(form.getRedirectTo());
    }

    private static String safeRedirectTarget(String candidate) {
        if (candidate == null || !candidate.startsWith("/") || candidate.startsWith("//")) {
            return "/";
        }
        return candidate;
    }
}
