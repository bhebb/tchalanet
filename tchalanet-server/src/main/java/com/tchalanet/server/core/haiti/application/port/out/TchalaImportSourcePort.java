package com.tchalanet.server.core.haiti.application.port.out;

import com.tchalanet.server.core.haiti.application.command.model.ImportTchalaEntriesCommand.ImportRow;
import java.util.List;

public interface TchalaImportSourcePort {
  List<ImportRow> readRows(String payloadRef);
}
