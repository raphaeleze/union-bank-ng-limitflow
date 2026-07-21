const formatter = new Intl.NumberFormat("en-NG", {
  style: "currency",
  currency: "NGN",
  maximumFractionDigits: 0,
});

const THIN_SPACE = " ";

export function formatCurrency(amount: number): string {
  return formatter
    .formatToParts(amount)
    .map((part) => (part.type === "currency" ? part.value + THIN_SPACE : part.value))
    .join("");
}
