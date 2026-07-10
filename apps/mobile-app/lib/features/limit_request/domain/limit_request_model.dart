class TimelineStepModel {
  const TimelineStepModel({required this.label, required this.status});

  factory TimelineStepModel.fromJson(Map<String, dynamic> json) => TimelineStepModel(
        label: json['label'] as String,
        status: json['status'] as String,
      );

  final String label;

  /// One of `COMPLETE`, `CURRENT`, `PENDING`.
  final String status;
}

class LimitRequestModel {
  const LimitRequestModel({
    required this.id,
    required this.accountId,
    required this.currentLimit,
    required this.requestedLimit,
    required this.reason,
    required this.status,
    required this.riskLevel,
    required this.createdAt,
    required this.updatedAt,
    required this.timeline,
  });

  factory LimitRequestModel.fromJson(Map<String, dynamic> json) => LimitRequestModel(
        id: json['id'] as String,
        accountId: json['accountId'] as String,
        currentLimit: (json['currentLimit'] as num).toDouble(),
        requestedLimit: (json['requestedLimit'] as num).toDouble(),
        reason: json['reason'] as String,
        status: json['status'] as String,
        riskLevel: json['riskLevel'] as String?,
        createdAt: DateTime.parse(json['createdAt'] as String),
        updatedAt: DateTime.parse(json['updatedAt'] as String),
        timeline: (json['timeline'] as List<dynamic>? ?? [])
            .map((e) => TimelineStepModel.fromJson(e as Map<String, dynamic>))
            .toList(),
      );

  final String id;
  final String accountId;
  final double currentLimit;
  final double requestedLimit;
  final String reason;
  final String status;
  final String? riskLevel;
  final DateTime createdAt;
  final DateTime updatedAt;
  final List<TimelineStepModel> timeline;

  bool get isUnderManualReview => status == 'UNDER_REVIEW';
  bool get isResolved => status == 'APPROVED' || status == 'REJECTED';
}

class CurrentLimitModel {
  const CurrentLimitModel({
    required this.accountId,
    required this.dailyLimit,
    required this.usedToday,
    required this.remaining,
    required this.activeRequest,
  });

  factory CurrentLimitModel.fromJson(Map<String, dynamic> json) => CurrentLimitModel(
        accountId: json['accountId'] as String,
        dailyLimit: (json['dailyLimit'] as num).toDouble(),
        usedToday: (json['usedToday'] as num).toDouble(),
        remaining: (json['remaining'] as num).toDouble(),
        activeRequest: json['activeRequest'] == null
            ? null
            : LimitRequestModel.fromJson(json['activeRequest'] as Map<String, dynamic>),
      );

  final String accountId;
  final double dailyLimit;
  final double usedToday;
  final double remaining;
  final LimitRequestModel? activeRequest;
}
