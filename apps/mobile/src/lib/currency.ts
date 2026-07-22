const formatter = new Intl.NumberFormat("en-NG", {
  style: "currency",
  currency: "NGN",
  maximumFractionDigits: 0,
});

const THIN_SPACE = " ";

export function formatCurrency(amount: number): string {
  // en-NG glues the ₦ symbol directly to the first digit with no space, which reads as
  // overlapping glyphs in the tabular-mono figures used throughout the app. A thin space
  // gives the symbol breathing room without visibly widening the figure.
  return formatter
    .formatToParts(amount)
    .map((part) => (part.type === "currency" ? part.value + THIN_SPACE : part.value))
    .join("");
}
