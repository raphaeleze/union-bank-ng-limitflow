import 'package:intl/intl.dart';

abstract class CurrencyFormatter {
  static final _format = NumberFormat.currency(locale: 'en_NG', symbol: '₦', decimalDigits: 0);

  static String format(num amount) => _format.format(amount);
}
