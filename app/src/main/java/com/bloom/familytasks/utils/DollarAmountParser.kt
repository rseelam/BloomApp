package com.bloom.familytasks.utils

object DollarAmountParser {
    /**
     * Extracts dollar amount from a text string
     * Handles formats like: $5, $10.50, 5 dollars, 10 bucks, etc.
     * Returns the amount as Int (in dollars), or null if not found
     */
    fun extractDollarAmount(text: String): Int? {
        // Pattern to match dollar amounts
        val patterns = listOf(
            Regex("""\$(\d+(?:\.\d{2})?)"""), // $5, $5.00, $10.50
            Regex("""(\d+(?:\.\d{2})?)\s*(?:dollar|buck)s?""", RegexOption.IGNORE_CASE), // 5 dollars, 10 bucks
            Regex("""pay\s*(\d+(?:\.\d{2})?)\s*(?:dollar|buck)?s?""", RegexOption.IGNORE_CASE), // pay 5
            Regex("""worth\s*\$?(\d+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE), // worth $5, worth 5
            Regex("""reward\s*(?:of\s*)?\$?(\d+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE) // reward of $5
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1]
                val amount = amountStr.toDoubleOrNull() ?: continue

                // Round to nearest dollar, with min $1 and max $20
                return amount.toInt().coerceIn(1, 20)
            }
        }

        return null
    }

    /**
     * Removes dollar amount references from text to get clean description
     */
    fun removeDollarReferences(text: String): String {
        return text
            .replace(Regex("""\$\d+(?:\.\d{2})?"""), "")
            .replace(Regex("""\d+(?:\.\d{2})?\s*(?:dollar|buck)s?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""pay\s*\d+(?:\.\d{2})?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""worth\s*\$?\d+(?:\.\d{2})?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""reward\s*(?:of\s*)?\$?\d+(?:\.\d{2})?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ") // Clean up extra spaces
            .trim()
    }
}