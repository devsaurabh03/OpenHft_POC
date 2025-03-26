package org.zerogc;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents an option symbol in the financial markets.
 * This class encapsulates all components needed to uniquely identify an option contract.
 */
public class OptionSymbol implements Serializable {
    private String underlyingSymbol;  // The ticker symbol of the underlying asset (e.g., AAPL)
    private OptionType optionType;    // PUT or CALL
    private LocalDate expirationDate; // When the option expires
    private BigDecimal strikePrice;   // The strike price of the option

    public OptionSymbol() {

    }

    /**
     * Enum representing the type of option
     */
    public enum OptionType {
        CALL,
        PUT
    }

    /**
     * Constructor for creating an option symbol
     */
    public OptionSymbol(String underlyingSymbol, OptionType optionType,
                        LocalDate expirationDate, BigDecimal strikePrice) {
        this.underlyingSymbol = underlyingSymbol;
        this.optionType = optionType;
        this.expirationDate = expirationDate;
        this.strikePrice = strikePrice;
    }

    /**
     * Format the option symbol in the standard format:
     * Underlying + Expiration Date + Option Type + Strike Price
     * Example: AAPL240621C00650000 (Apple $650 call option expiring June 21, 2024)
     */
    public String formatSymbol() {
        // Format: UnderlyingSymbol + YY + MM + DD + C/P + StrikePrice(padded)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String dateStr = expirationDate.format(formatter);
        String typeChar = optionType == OptionType.CALL ? "C" : "P";

        // Format strike price: multiply by 1000, remove decimal, pad to 8 digits
        String strikeStr = String.format("%08d", strikePrice.multiply(BigDecimal.valueOf(1000)).intValue());

        return underlyingSymbol + dateStr + typeChar + strikeStr;
    }

    /**
     * Parse an option symbol string into an OptionSymbol object
     */
    public static OptionSymbol parseSymbol(String symbolStr) {
        // Validation and parsing logic
        if (symbolStr == null || symbolStr.length() < 15) {
            throw new IllegalArgumentException("Invalid option symbol format");
        }

        // Extract components (this is simplified and would need proper regex in production)
        String ticker = symbolStr.replaceAll("[0-9].*$", "");

        // Parse date (positions may vary based on ticker length)
        int dateStart = ticker.length();
        String dateStr = symbolStr.substring(dateStart, dateStart + 6);
        LocalDate expDate = LocalDate.parse("20" + dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Parse option type
        char typeChar = symbolStr.charAt(dateStart + 6);
        OptionType type = typeChar == 'C' ? OptionType.CALL : OptionType.PUT;

        // Parse strike price
        String strikeStr = symbolStr.substring(dateStart + 7);
        BigDecimal strike = new BigDecimal(Integer.parseInt(strikeStr)).divide(BigDecimal.valueOf(1000));

        return new OptionSymbol(ticker, type, expDate, strike);
    }

    // Getters and setters
    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public void setUnderlyingSymbol(String underlyingSymbol) {
        this.underlyingSymbol = underlyingSymbol;
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public void setOptionType(OptionType optionType) {
        this.optionType = optionType;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public BigDecimal getStrikePrice() {
        return strikePrice;
    }

    public void setStrikePrice(BigDecimal strikePrice) {
        this.strikePrice = strikePrice;
    }

    @Override
    public String toString() {
        return String.format("%s %s $%.2f %s",
                underlyingSymbol,
                expirationDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                strikePrice,
                optionType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptionSymbol that = (OptionSymbol) o;
        return Objects.equals(underlyingSymbol, that.underlyingSymbol) &&
                optionType == that.optionType &&
                Objects.equals(expirationDate, that.expirationDate) &&
                Objects.equals(strikePrice, that.strikePrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(underlyingSymbol, optionType, expirationDate, strikePrice);
    }
}
