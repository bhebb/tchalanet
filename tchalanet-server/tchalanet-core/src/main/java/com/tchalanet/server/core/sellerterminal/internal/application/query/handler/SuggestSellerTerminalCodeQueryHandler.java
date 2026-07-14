package com.tchalanet.server.core.sellerterminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalCodeSuggestionView;
import com.tchalanet.server.core.sellerterminal.api.query.SuggestSellerTerminalCodeQuery;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SuggestSellerTerminalCodeQueryHandler
    implements QueryHandler<SuggestSellerTerminalCodeQuery, SellerTerminalCodeSuggestionView> {

  private static final Pattern POS_CODE = Pattern.compile("^POS-(\\d+)$", Pattern.CASE_INSENSITIVE);

  private final SellerTerminalReaderPort reader;

  @Override
  public SellerTerminalCodeSuggestionView handle(SuggestSellerTerminalCodeQuery q) {
    int next =
        reader.terminalCodes(q.tenantId()).stream()
                .map(SuggestSellerTerminalCodeQueryHandler::posSequence)
                .filter(OptionalInt::isPresent)
                .mapToInt(OptionalInt::getAsInt)
                .max()
                .orElse(0)
            + 1;

    return new SellerTerminalCodeSuggestionView("POS-%03d".formatted(next));
  }

  private static OptionalInt posSequence(String terminalCode) {
    if (terminalCode == null) return OptionalInt.empty();
    var matcher = POS_CODE.matcher(terminalCode.trim());
    if (!matcher.matches()) return OptionalInt.empty();
    try {
      return OptionalInt.of(Integer.parseInt(matcher.group(1)));
    } catch (NumberFormatException ignored) {
      return OptionalInt.empty();
    }
  }
}
