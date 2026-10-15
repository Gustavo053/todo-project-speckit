package com.todoproject.adapter.in.web;

import com.todoproject.adapter.in.web.dto.ThemeForm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the current appearance preference available to every page's layout, so the control present
 * on every screen (FR-020) always reflects the stored choice without each controller fetching it.
 */
@ControllerAdvice(basePackages = "com.todoproject.adapter.in.web")
class AppearanceModelAdvice {

    @ModelAttribute("appearance")
    AppearancePreference appearance(HttpServletRequest request) {
        return ThemeCookie.read(request);
    }

    @ModelAttribute("themeForm")
    ThemeForm themeForm(HttpServletRequest request) {
        ThemeForm form = new ThemeForm();
        form.setPreference(ThemeCookie.read(request).next().name());
        form.setRedirectTo(request.getRequestURI());
        return form;
    }
}
