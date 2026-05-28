package com.tchalanet.server.features.pagemodel.contract;

/**
 * Top-level shell fragment — the complete shell payload for a private dashboard surface.
 *
 * <p>Served by {@code JsonFileProvider} from the surface-specific shell fragment
 * (e.g. {@code private_shell_cashier.json}, {@code private_shell_tenantadmin.json}).
 * This record is the typed Java representation of those fragments.
 *
 * <p>{@code footer} is {@code null} in V1 — reserved for future footer navigation.
 */
public record ShellFragment(
    TopAppBar topAppBar,
    NavigationDrawer navigationDrawer,
    Object footer) {

  public static ShellFragment of(TopAppBar topAppBar, NavigationDrawer navigationDrawer) {
    return new ShellFragment(topAppBar, navigationDrawer, null);
  }
}
