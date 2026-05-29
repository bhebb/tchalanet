package com.tchalanet.server.features.pagemodel.contract;

import java.util.List;

/**
 * Public header payload — brand + primary nav links + secondary links + CTA actions.
 *
 * <p>Served by {@code JsonFileProvider} from {@code public_header_links.json}.
 * This record is the typed Java representation of that fragment.
 *
 * <p>Per {@code harden-pagemodel-security-v2} D1: public headers MUST NOT expose
 * private routes or admin / superadmin metadata.
 */
public record PublicHeader(
    BrandBlock brand,
    List<NavigationEntry> primary,
    List<NavigationEntry> secondary,
    List<NavigationEntry> actions) {}
