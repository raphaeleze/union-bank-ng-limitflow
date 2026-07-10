import 'package:flutter_test/flutter_test.dart';
import 'package:limitflow_mobile/features/limit_request/domain/limit_request_model.dart';

void main() {
  group('LimitRequestModel.fromJson', () {
    test('parses a full backend response, including the timeline', () {
      final json = {
        'id': '55555555-5555-5555-5555-555555555555',
        'accountId': '44444444-4444-4444-4444-444444444444',
        'currentLimit': 200000.0,
        'requestedLimit': 500000.0,
        'reason': 'Sending funds to a family member for a medical emergency',
        'status': 'UNDER_REVIEW',
        'riskLevel': 'MEDIUM',
        'createdAt': '2026-07-10T16:51:41.701083Z',
        'updatedAt': '2026-07-10T16:58:41.701083Z',
        'timeline': [
          {'label': 'Submitted', 'status': 'COMPLETE'},
          {'label': 'OTP Verified', 'status': 'COMPLETE'},
          {'label': 'Risk Assessment', 'status': 'CURRENT'},
        ],
      };

      final model = LimitRequestModel.fromJson(json);

      expect(model.id, '55555555-5555-5555-5555-555555555555');
      expect(model.currentLimit, 200000.0);
      expect(model.requestedLimit, 500000.0);
      expect(model.status, 'UNDER_REVIEW');
      expect(model.riskLevel, 'MEDIUM');
      expect(model.timeline, hasLength(3));
      expect(model.timeline.last.status, 'CURRENT');
      expect(model.isUnderManualReview, isTrue);
      expect(model.isResolved, isFalse);
    });

    test('treats a null riskLevel and missing timeline as not-yet-assessed', () {
      final json = {
        'id': '55555555-5555-5555-5555-555555555555',
        'accountId': '44444444-4444-4444-4444-444444444444',
        'currentLimit': 200000.0,
        'requestedLimit': 250000.0,
        'reason': 'Paying a contractor',
        'status': 'OTP_PENDING',
        'riskLevel': null,
        'createdAt': '2026-07-10T16:51:41.701083Z',
        'updatedAt': '2026-07-10T16:51:41.701083Z',
      };

      final model = LimitRequestModel.fromJson(json);

      expect(model.riskLevel, isNull);
      expect(model.timeline, isEmpty);
      expect(model.isUnderManualReview, isFalse);
      expect(model.isResolved, isFalse);
    });

    test('recognizes APPROVED and REJECTED as resolved', () {
      final approved = LimitRequestModel.fromJson({
        'id': '1',
        'accountId': 'a',
        'currentLimit': 1.0,
        'requestedLimit': 2.0,
        'reason': 'r',
        'status': 'APPROVED',
        'riskLevel': 'LOW',
        'createdAt': '2026-07-10T16:51:41.701083Z',
        'updatedAt': '2026-07-10T16:51:41.701083Z',
      });
      final rejected = LimitRequestModel.fromJson({
        'id': '2',
        'accountId': 'a',
        'currentLimit': 1.0,
        'requestedLimit': 2.0,
        'reason': 'r',
        'status': 'REJECTED',
        'riskLevel': 'HIGH',
        'createdAt': '2026-07-10T16:51:41.701083Z',
        'updatedAt': '2026-07-10T16:51:41.701083Z',
      });

      expect(approved.isResolved, isTrue);
      expect(rejected.isResolved, isTrue);
    });
  });
}
