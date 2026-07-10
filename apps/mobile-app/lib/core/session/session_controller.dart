import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../storage/secure_storage_service.dart';
import 'app_user.dart';

enum SessionStatus { unknown, authenticated, unauthenticated }

class SessionState {
  const SessionState({required this.status, this.user});

  const SessionState.unknown() : this(status: SessionStatus.unknown);

  final SessionStatus status;
  final AppUser? user;

  bool get isAuthenticated => status == SessionStatus.authenticated;

  SessionState copyWith({SessionStatus? status, AppUser? user}) {
    return SessionState(status: status ?? this.status, user: user ?? this.user);
  }
}

/// Single source of truth for "are we logged in, and as whom" — read both by
/// the router (to decide splash/login vs. the app shell) and by [ApiClient]
/// (to force a logout when the backend returns a 401).
class SessionController extends StateNotifier<SessionState> {
  SessionController(this._secureStorage) : super(const SessionState.unknown());

  final SecureStorageService _secureStorage;

  Future<void> restore(AppUser? cachedUser) async {
    final token = await _secureStorage.readToken();
    if (token != null && cachedUser != null) {
      state = SessionState(status: SessionStatus.authenticated, user: cachedUser);
    } else {
      state = const SessionState(status: SessionStatus.unauthenticated);
    }
  }

  Future<void> login({required String token, required AppUser user}) async {
    await _secureStorage.saveToken(token);
    state = SessionState(status: SessionStatus.authenticated, user: user);
  }

  Future<void> logout() async {
    await _secureStorage.clearToken();
    state = const SessionState(status: SessionStatus.unauthenticated);
  }
}
