<#-- Tchalanet custom register template extends Keycloak base -->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayWide=false>
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
    <div id="kc-register" class="kcFormCard">
      <div id="kc-register-form">
        <form id="kc-register-form" action="${url.registrationAction}" method="post">
          <div class="form-group">
            <label for="firstName">${msg("firstName")}</label>
            <input type="text" id="firstName" name="firstName" value="${(register.formData.firstName!'')}" />
          </div>
          <div class="form-group">
            <label for="lastName">${msg("lastName")}</label>
            <input type="text" id="lastName" name="lastName" value="${(register.formData.lastName!'')}" />
          </div>
          <div class="form-group">
            <label for="email">${msg("email")}</label>
            <input type="email" id="email" name="email" value="${(register.formData.email!'')}" autocomplete="email" />
          </div>
          <div class="form-group">
            <label for="password">${msg("password")}</label>
            <input type="password" id="password" name="password" autocomplete="new-password" />
          </div>
          <div class="form-group">
            <label for="password-confirm">${msg("passwordConfirm")}</label>
            <input type="password" id="password-confirm" name="password-confirm" autocomplete="new-password" />
          </div>
          <div class="form-group">
            <input class="btn btn-primary" type="submit" value="${msg("doRegister")}" />
          </div>
        </form>
      </div>
    </div>
  </#nested>

  <#-- Info -->
  <#nested "info">
    <div class="kc-info-text">
      ${msg("backToLogin")} <a href="${url.loginUrl}">${msg("doLogIn")}</a>
    </div>
  </#nested>
</@layout.registrationLayout>

