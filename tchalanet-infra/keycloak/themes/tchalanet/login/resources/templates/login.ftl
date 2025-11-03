<#-- Tchalanet custom login template extends Keycloak base -->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo displayWide=usernameEditDisabled??>
  <#if realm.internationalizationEnabled>
    <#assign locales = locale.supported>
  </#if>
  <#-- Header -->
  <#nested "header">
    <div id="kc-header" class="kc-header">
      <div id="kc-header-wrapper" class="kc-header-wrapper">
        <img src="${url.resourcesPath}/img/logo.svg" alt="Tchalanet" style="height:40px;">
        <span class="kc-logo-text">Tchalanet</span>
      </div>
    </div>
  </#nested>

  <#-- Form -->
  <#nested "form">
    <div id="kc-form" class="kcFormCard">
      <div id="kc-form-wrapper">
        <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
          <div class="form-group">
            <label for="username">${msg("username")}</label>
            <input tabindex="1" id="username" class="form-control" name="username" value="${(login.username!'')}" type="text" autofocus autocomplete="username">
          </div>
          <div class="form-group">
            <label for="password">${msg("password")}</label>
            <input tabindex="2" id="password" class="form-control" name="password" type="password" autocomplete="current-password">
          </div>
          <div class="form-group">
            <input class="btn btn-primary" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
          </div>
          <#if realm.resetPasswordAllowed>
            <div class="form-group">
              <a href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a>
            </div>
          </#if>
        </form>
      </div>
    </div>
  </#nested>

  <#-- Info / Social -->
  <#nested "info">
    <#if realm.registrationAllowed>
      <div class="kc-info-text">
        ${msg("noAccount")} <a href="${url.registrationUrl}">${msg("doRegister")}</a>
      </div>
    </#if>
  </#nested>
</@layout.registrationLayout>
