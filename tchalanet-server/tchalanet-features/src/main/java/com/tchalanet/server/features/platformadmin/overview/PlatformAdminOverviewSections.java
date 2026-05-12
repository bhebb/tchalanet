package com.tchalanet.server.features.platformadmin.overview;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlatformAdminOverviewSections {

  public List<PlatformAdminOverviewView.SectionStatusItem> items() {
    return List.of(
        new PlatformAdminOverviewView.SectionStatusItem("game", true, "/platform-admin/game/overview"),
        new PlatformAdminOverviewView.SectionStatusItem("resultslot", true, "/platform-admin/resultslot/overview"),
        new PlatformAdminOverviewView.SectionStatusItem("plan", false, "/platform-admin/plan/overview")
    );
  }
}
