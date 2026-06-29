package com.attentionmirror.domain

/**
 * Display currency. Values are computed in an INR base (our CPM table) and
 * multiplied by [factor] for display. The factor folds together FX *and* a
 * regional ad-market tier (high-income markets have far higher CPMs), so a US
 * user sees realistically higher figures than an India user — not a naive FX
 * conversion. All figures remain estimates. See docs/ASSUMPTIONS.md.
 */
data class Currency(val code: String, val symbol: String, val factor: Double)

object Currencies {
    val INR = Currency("INR", "₹", 1.0)

    // factor ≈ (INR→local FX) × market tier (high ≈ ×4, mid ≈ ×2, low ≈ ×1).
    val ALL: List<Currency> = listOf(
        INR,
        Currency("USD", "$", 0.048),
        Currency("EUR", "€", 0.044),
        Currency("GBP", "£", 0.038),
        Currency("CAD", "C$", 0.064),
        Currency("AUD", "A$", 0.072),
        Currency("SGD", "S$", 0.064),
        Currency("AED", "AED ", 0.176),
        Currency("JPY", "¥", 7.2),
        Currency("BRL", "R$", 0.138),
        Currency("MXN", "MX$", 0.40),
        Currency("ZAR", "R", 0.44),
        Currency("PHP", "₱", 0.67),
        Currency("PKR", "₨", 3.3),
        Currency("BDT", "৳", 1.3),
        Currency("IDR", "Rp", 190.0),
        Currency("NGN", "₦", 18.0),
    )

    val BY_CODE: Map<String, Currency> = ALL.associateBy { it.code }

    private val COUNTRY_TO_CODE: Map<String, String> = mapOf(
        "IN" to "INR", "US" to "USD", "GB" to "GBP", "CA" to "CAD", "AU" to "AUD",
        "SG" to "SGD", "AE" to "AED", "JP" to "JPY", "BR" to "BRL", "MX" to "MXN",
        "ZA" to "ZAR", "PH" to "PHP", "PK" to "PKR", "BD" to "BDT", "ID" to "IDR",
        "NG" to "NGN",
        // Eurozone
        "DE" to "EUR", "FR" to "EUR", "ES" to "EUR", "IT" to "EUR", "NL" to "EUR",
        "IE" to "EUR", "PT" to "EUR", "AT" to "EUR", "BE" to "EUR", "FI" to "EUR",
        "GR" to "EUR",
    )

    /** Map an ISO country code (e.g. "US") to a currency; defaults to INR. */
    fun forCountry(countryCode: String?): Currency {
        val code = COUNTRY_TO_CODE[countryCode?.uppercase()] ?: return INR
        return BY_CODE[code] ?: INR
    }
}
