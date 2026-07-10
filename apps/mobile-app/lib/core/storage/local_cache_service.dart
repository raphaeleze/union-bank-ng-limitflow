import 'dart:convert';

import 'package:hive_flutter/hive_flutter.dart';

/// Simple JSON-blob cache built on Hive, used to keep the dashboard usable
/// (read-only) for a moment while offline or on a slow connection.
///
/// Deliberately avoids generated Hive type adapters: everything is stored as
/// a JSON string in a single untyped box, decoded back into a `Map` on read.
class LocalCacheService {
  static const _boxName = 'limitflow_cache';

  late Box<String> _box;

  Future<void> init() async {
    await Hive.initFlutter();
    _box = await Hive.openBox<String>(_boxName);
  }

  Future<void> writeJson(String key, Map<String, dynamic> value) async {
    await _box.put(key, jsonEncode(value));
  }

  Map<String, dynamic>? readJson(String key) {
    final raw = _box.get(key);
    if (raw == null) return null;
    return jsonDecode(raw) as Map<String, dynamic>;
  }

  Future<void> clear() => _box.clear();
}
