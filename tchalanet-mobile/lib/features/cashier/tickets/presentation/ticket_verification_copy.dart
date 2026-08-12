import '../../../../core/i18n/i18n_models.dart';

const _htVerificationCopy = {
  'pos.ticket.verify.payable.title': 'Tikè gayan',
  'pos.ticket.verify.payable.message':
      'Montan ki genyen an: {amount} {currency}.',
  'pos.ticket.verify.already_paid.title': 'Tikè deja peye',
  'pos.ticket.verify.already_paid.message': 'Tikè sa a deja regle.',
  'pos.ticket.verify.not_payable_lost.title': 'Tikè pa genyen',
  'pos.ticket.verify.not_payable_lost.message': 'Tikè sa a pa genyen.',
  'pos.ticket.verify.not_payable_pending_draw.title': 'Tiraj poko fèt',
  'pos.ticket.verify.not_payable_pending_draw.message':
      'Tann tiraj la anvan w verifye anko.',
  'pos.ticket.verify.not_payable_result_pending.title': 'Rezilta ap tann',
  'pos.ticket.verify.not_payable_result_pending.message':
      'Rezilta tiraj la poko disponib.',
  'pos.ticket.verify.cancelled.title': 'Tikè anile',
  'pos.ticket.verify.cancelled.message': 'Tikè sa a anile.',
  'pos.ticket.verify.voided.title': 'Tikè invalid',
  'pos.ticket.verify.voided.message': 'Tikè sa a pa valab anko.',
  'pos.ticket.verify.not_found.title': 'Tikè pa jwenn',
  'pos.ticket.verify.not_found.message': 'Nou pa jwenn tikè sa a.',
  'pos.ticket.verify.blocked.title': 'Tikè bloke',
  'pos.ticket.verify.blocked.message': 'Kontakte administratè a.',
  'pos.ticket.verify.repair_required.title': 'Verifikasyon nesesè',
  'pos.ticket.verify.repair_required.message':
      'Kontakte administratè a anvan w kontinye.',
  'pos.ticket.verify.operation_not_allowed.title': 'Aksyon pa otorize',
  'pos.ticket.verify.operation_not_allowed.message':
      'Tèminal sa a pa ka verifye tikè.',
  'pos.ticket.verify.unknown.title': 'Verifikasyon tikè',
  'pos.ticket.verify.unknown.message': 'Nou pa kapab detèmine estati tikè a.',
};

const _legacyFrenchVerificationCopy = {
  'ticket gagnant': 'Tikè gayan',
  'montant gagnant: {amount} {currency}.':
      'Montan ki genyen an: {amount} {currency}.',
  'ticket déjà payé': 'Tikè deja peye',
  'ce ticket est déjà réglé.': 'Tikè sa a deja regle.',
  'ticket non gagnant': 'Tikè pa genyen',
  "ce ticket n'est pas payable.": 'Tikè sa a pa genyen.',
  'tirage en attente': 'Tiraj poko fèt',
  'attendez le tirage avant de vérifier à nouveau.':
      'Tann tiraj la anvan w verifye anko.',
  'résultat en attente': 'Rezilta ap tann',
  "le résultat de ce ticket n'est pas encore disponible.":
      'Rezilta tiraj la poko disponib.',
  'ticket annulé': 'Tikè anile',
  'ce ticket est annulé.': 'Tikè sa a anile.',
  'ticket invalidé': 'Tikè invalid',
  "ce ticket n'est plus valide.": 'Tikè sa a pa valab anko.',
  'ticket introuvable': 'Tikè pa jwenn',
  'nous ne trouvons pas ce ticket.': 'Nou pa jwenn tikè sa a.',
  'ticket bloqué': 'Tikè bloke',
  "contactez l'administrateur.": 'Kontakte administratè a.',
  'vérification requise': 'Verifikasyon nesesè',
  "contactez l'administrateur avant de continuer.":
      'Kontakte administratè a anvan w kontinye.',
  'action non autorisée': 'Aksyon pa otorize',
  'ce terminal ne peut pas vérifier les tickets.':
      'Tèminal sa a pa ka verifye tikè.',
  'vérification du ticket': 'Verifikasyon tikè',
  'nous ne pouvons pas déterminer le statut du ticket.':
      'Nou pa kapab detèmine estati tikè a.',
};

String translateTicketVerificationCopy(
  I18nBundle translations,
  String? keyOrText, {
  required String fallback,
  Map<String, dynamic>? params,
}) {
  final text = _resolveTicketVerificationCopy(
    translations,
    keyOrText,
    fallback,
  );
  return (params ?? const {}).entries.fold<String>(
    text,
    (current, entry) => current.replaceAll('{${entry.key}}', '${entry.value}'),
  );
}

String _resolveTicketVerificationCopy(
  I18nBundle translations,
  String? keyOrText,
  String fallback,
) {
  final candidate = keyOrText?.trim();
  if (translations.locale == 'ht') {
    final local = _htLookup(candidate) ?? _htLookup(fallback);
    if (local != null) return local;
  }

  final translated = candidate == null || candidate.isEmpty
      ? translations.translate(fallback)
      : translations.translate(candidate);
  if (translated != candidate) return translated;
  return translations.translate(fallback);
}

String? _htLookup(String? keyOrText) {
  if (keyOrText == null || keyOrText.isEmpty) return null;
  return _htVerificationCopy[keyOrText] ??
      _legacyFrenchVerificationCopy[keyOrText.toLowerCase()];
}
