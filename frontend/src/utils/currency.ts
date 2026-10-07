/**
 * Centralized currency formatting utility for ShopSphere.
 * Formats monetary amounts using Indian locale (en-IN) and INR currency (₹).
 */
const inrFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/**
 * Format a numeric amount or numeric string into Indian Rupee format (e.g. ₹149.99).
 * Preserves the exact numeric value.
 */
export const formatCurrency = (amount: number | string | null | undefined): string => {
  if (amount === null || amount === undefined || amount === '') {
    return inrFormatter.format(0);
  }
  const numericAmount = typeof amount === 'string' ? Number(amount) : amount;
  if (isNaN(numericAmount)) {
    return inrFormatter.format(0);
  }
  return inrFormatter.format(numericAmount);
};
