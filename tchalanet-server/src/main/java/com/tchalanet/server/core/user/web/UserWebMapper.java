package com.tchalanet.server.core.user.web;

import com.tchalanet.server.core.user.domain.model.AppUser;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserWebMapper {

  @Mapping(target = "lastLoginAt", ignore = true)
  AppUser toDomain(UserAdminController.CreateUserRequest req);

  @Mapping(target = "lastLoginAt", ignore = true)
  AppUser updateToDomain(UUID id, UserAdminController.UpdateUserRequest req);

  @Mapping(target = "id", source = "id")
  @Mapping(target = "tenantId", source = "tenantId")
  @Mapping(target = "username", source = "username")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "displayName", source = "displayName")
  @Mapping(target = "locale", source = "locale")
  @Mapping(target = "lastLoginAt", source = "lastLoginAt")
  UserAdminController.UserResponse toResponse(AppUser user);
}
