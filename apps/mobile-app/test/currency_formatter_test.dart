import 'package:flutter_test/flutter_test.dart';
import 'package:limitflow_mobile/core/utils/currency_formatter.dart';

void main() {
  group('CurrencyFormatter', () {
    test('formats whole numbers with the naira symbol and no decimals', () {
      expect(CurrencyFormatter.format(200000), '₦200,000');
    });

    test('rounds fractional amounts rather than showing decimals', () {
      expect(CurrencyFormatter.format(180000.75), '₦180,001');
    });

    test('formats zero', () {
      expect(CurrencyFormatter.format(0), '₦0');
    });
  });
}
