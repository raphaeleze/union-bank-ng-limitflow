const formatter = new Intl.NumberFormat("en-NG", {
  style: "currency",
  currency: "NGN",
  maximumFractionDigits: 0,
});

const THIN_SPACE = " ";

export function formatCurrency(amount: number): string {
  // en-NG glues the ₦ symbol directly to the first digit with no space, which reads as
  // overlapping glyphs in the tabular-mono figures used throughout the app. A thin space
  // gives the symbol breathing room without visibly widening the figure.
  //
  // Intl.NumberFormat.prototype.formatToParts isn't reliably implemented in React
  // Native's Hermes engine (it type-checks fine, but throws "undefined is not a
  // function" at runtime on-device) even though plain .format() is — so this inserts
  // the thin space with a regex against the formatted string instead of formatToParts.
  return formatter.format(amount).replace(/^(\D+)/, `$1${THIN_SPACE}`);
}
