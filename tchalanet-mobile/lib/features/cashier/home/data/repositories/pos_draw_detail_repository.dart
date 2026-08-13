import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/pos_draw_detail_models.dart';
import '../services/pos_draw_detail_service.dart';

class PosDrawDetailRepository {
  const PosDrawDetailRepository(this._service);

  final PosDrawDetailService _service;

  Future<PosDrawDetailResponse> fetchDrawDetail(String drawId) =>
      _service.fetchDrawDetail(drawId);
}

final posDrawDetailRepositoryProvider = Provider<PosDrawDetailRepository>((
  ref,
) {
  return PosDrawDetailRepository(ref.watch(posDrawDetailServiceProvider));
});
