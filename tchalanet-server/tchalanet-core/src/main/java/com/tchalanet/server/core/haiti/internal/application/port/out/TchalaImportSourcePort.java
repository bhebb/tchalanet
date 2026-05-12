package com.tchalanet.server.core.haiti.internal.application.port.out;

import com.tchalanet.server.core.haiti.api.command.ImportTchalaEntriesCommand.ImportRow;
import java.util.List;

public interface TchalaImportSourcePort {
  List<ImportRow> readRows(String payloadRef);
}
