package com.tchalanet.server.core.user.domain.usecase;

import com.tchalanet.server.core.user.domain.model.AppUser;
import java.util.List;

public interface ListAllUsersUseCase {
  List<AppUser> listAll();
}
