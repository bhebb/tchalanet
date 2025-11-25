package com.tchalanet.server.user.domain.usecase;

import com.tchalanet.server.user.domain.model.AppUser;
import java.util.List;

public interface ListAllUsersUseCase {
  List<AppUser> listAll();
}
