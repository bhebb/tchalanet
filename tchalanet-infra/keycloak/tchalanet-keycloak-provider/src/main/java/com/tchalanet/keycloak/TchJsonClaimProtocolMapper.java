package com.tchalanet.keycloak;

import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.*;
import java.util.stream.Collectors;

public class TchJsonClaimProtocolMapper extends AbstractOIDCProtocolMapper
  implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

  public static final String PROVIDER_ID = "tch-json-claim-mapper";
  private static final String CFG_CLAIM_NAME = "claim.name";             // ex: "tch"
  private static final String CFG_JSON_TYPE = "jsonType.label";          // "JSON"
  private static final String CFG_ID_TOKEN  = "id.token.claim";
  private static final String CFG_AT_TOKEN  = "access.token.claim";
  private static final String CFG_USERINFO  = "userinfo.token.claim";

  @Override public String getId() { return PROVIDER_ID; }
  @Override public String getDisplayType() { return "Tch JSON Claim"; }
  @Override public String getDisplayCategory() { return "Token mapper"; }
  @Override public String getHelpText() {
    return "Injecte un claim JSON 'tch' (tenantId, plan, featureSetId, locale, roles, resourceRoles, groups).";
  }

  // Propriétés configurables dans l’UI (sans OIDCAttributeMapperHelper)
  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    List<ProviderConfigProperty> props = new ArrayList<>();

    ProviderConfigProperty claim = new ProviderConfigProperty();
    claim.setName(CFG_CLAIM_NAME);
    claim.setLabel("Claim name");
    claim.setHelpText("Nom du claim (ex: 'tch').");
    claim.setType(ProviderConfigProperty.STRING_TYPE);
    claim.setDefaultValue("tch");
    props.add(claim);

    ProviderConfigProperty jsonType = new ProviderConfigProperty();
    jsonType.setName(CFG_JSON_TYPE);
    jsonType.setLabel("JSON Type");
    jsonType.setHelpText("Laisser 'JSON' pour émettre un objet JSON.");
    jsonType.setType(ProviderConfigProperty.LIST_TYPE);
    jsonType.setOptions(List.of("JSON"));
    jsonType.setDefaultValue("JSON");
    props.add(jsonType);

    props.add(boolProp(CFG_ID_TOKEN, "Add to ID token", true));
    props.add(boolProp(CFG_AT_TOKEN, "Add to Access token", true));
    props.add(boolProp(CFG_USERINFO, "Add to Userinfo", true));
    return props;
  }

  private static ProviderConfigProperty boolProp(String name, String label, boolean defVal) {
    ProviderConfigProperty p = new ProviderConfigProperty();
    p.setName(name);
    p.setLabel(label);
    p.setType(ProviderConfigProperty.BOOLEAN_TYPE);
    p.setDefaultValue(Boolean.toString(defVal));
    return p;
  }

  @Override
  protected void setClaim(IDToken token,
                          ProtocolMapperModel mappingModel,
                          UserSessionModel userSession,
                          KeycloakSession session,
                          ClientSessionContext ctx) {

    UserModel user = userSession.getUser();
    RealmModel realm = userSession.getRealm();

    String tenantId = inferTenantFromGroups(user, "default");
    String plan = getAttr(user, "plan", "free");
    String featureSetId = getAttr(user, "featureSetId", "base");
    String locale = Optional.ofNullable(user.getFirstAttribute("locale"))
      .orElse(Optional.ofNullable(user.getFirstAttribute("locale_str")).orElse("fr"));

    // Realm roles
    Set<String> realmRoles = user.getRealmRoleMappingsStream()
      .map(RoleModel::getName)
      .collect(Collectors.toCollection(TreeSet::new));

    // Client roles (resourceRoles)
    Map<String, Set<String>> resourceRoles = new TreeMap<>();
    realm.getClientsStream().forEach(client -> {
      Set<String> roles = user.getClientRoleMappingsStream(client)
        .map(RoleModel::getName)
        .collect(Collectors.toCollection(TreeSet::new));
      if (!roles.isEmpty()) resourceRoles.put(client.getClientId(), roles);
    });

    // Group paths (reconstruits, KC 26)
    List<String> groups = user.getGroupsStream()
      .map(TchJsonClaimProtocolMapper::buildGroupPath)
      .sorted()
      .toList();

    Map<String, Object> tch = new LinkedHashMap<>();
    tch.put("tenantId", tenantId);
    tch.put("plan", plan);
    tch.put("featureSetId", featureSetId);
    tch.put("locale", locale);
    tch.put("roles", realmRoles);
    tch.put("resourceRoles", resourceRoles);
    tch.put("groups", groups);

    String claimName = mappingModel.getConfig().getOrDefault(CFG_CLAIM_NAME, "tch");
    token.getOtherClaims().put(claimName, tch);
  }

  private static String getAttr(UserModel user, String k, String defVal) {
    String v = user.getFirstAttribute(k);
    return (v != null && !v.isBlank()) ? v : defVal;
  }

  // KC 26: reconstruire le path
  private static String buildGroupPath(GroupModel g) {
    Deque<String> parts = new ArrayDeque<>();
    GroupModel cur = g;
    while (cur != null) { parts.addFirst(cur.getName()); cur = cur.getParent(); }
    return "/" + String.join("/", parts);
  }

  private static String inferTenantFromGroups(UserModel user, String defVal) {
    return user.getGroupsStream()
      .map(TchJsonClaimProtocolMapper::buildGroupPath)
      .filter(p -> p.startsWith("/tenants/"))
      .map(p -> { String[] s = p.split("/"); return s.length >= 3 ? s[2] : null; })
      .filter(Objects::nonNull)
      .findFirst().orElse(defVal);
  }

  // Fabrique utilitaire (optionnelle)
  public static ProtocolMapperModel create(String name) {
    ProtocolMapperModel m = new ProtocolMapperModel();
    m.setName(name);
    m.setProtocolMapper(PROVIDER_ID);
    m.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
    Map<String, String> cfg = new HashMap<>();
    cfg.put(CFG_CLAIM_NAME, "tch");
    cfg.put(CFG_JSON_TYPE, "JSON");
    cfg.put(CFG_ID_TOKEN, "true");
    cfg.put(CFG_AT_TOKEN, "true");
    cfg.put(CFG_USERINFO, "true");
    m.setConfig(cfg);
    return m;
  }
}
