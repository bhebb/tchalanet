package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.platform.tenant.api.model.view.TenantHolidayTemplateView;

import java.util.List;

final class TenantHolidayTemplates {

    private TenantHolidayTemplates() {}

    static List<TenantHolidayTemplateView> haitiV0() {
        return List.of(
            new TenantHolidayTemplateView("haiti_independence_day", "HT", "01-01", false, "Fete de l'independance"),
            new TenantHolidayTemplateView("haiti_ancestors_day", "HT", "01-02", false, "Jour des Aieux"),
            new TenantHolidayTemplateView("haiti_labour_agriculture_day", "HT", "05-01", false, "Fete du Travail et de l'Agriculture"),
            new TenantHolidayTemplateView("haiti_flag_university_day", "HT", "05-18", false, "Fete du Drapeau et de l'Universite"),
            new TenantHolidayTemplateView("haiti_dessalines_day", "HT", "10-17", false, "Mort de Dessalines"),
            new TenantHolidayTemplateView("haiti_all_saints_day", "HT", "11-01", false, "Toussaint"),
            new TenantHolidayTemplateView("haiti_all_souls_day", "HT", "11-02", false, "Jour des Morts"),
            new TenantHolidayTemplateView("christmas_day", "HT", "12-25", false, "Noel")
        );
    }
}
