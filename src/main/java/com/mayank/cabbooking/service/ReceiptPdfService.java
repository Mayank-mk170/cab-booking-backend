
package com.mayank.cabbooking.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class ReceiptPdfService {

    public byte[] generateReceiptPdf(
            Map<String, Object> receipt) {

        if (receipt == null || receipt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Receipt data is missing");
        }

        try (ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(output);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(
                    new Paragraph("RIDEGO")
                            .setBold()
                            .setFontSize(24));

            document.add(
                    new Paragraph("Ride Payment Receipt")
                            .setBold()
                            .setFontSize(16));

            document.add(new Paragraph(
                    "Receipt Number: "
                            + value(receipt, "receiptNumber")));

            document.add(new Paragraph(
                    "Ride ID: " + value(receipt, "rideId")));

            document.add(new Paragraph(
                    "Payment Date: "
                            + formatDate(receipt.get("paidAt"))));

            document.add(new Paragraph(" "));

            Table table = new Table(
                    UnitValue.createPercentArray(
                            new float[]{2, 3}))
                    .useAllAvailableWidth();

            addRow(table, "Description", "Details");

            addRow(
                    table,
                    "Amount",
                    formatAmount(
                            receipt.get("amount"),
                            value(receipt, "currency")));

            addRow(
                    table,
                    "Payment Method",
                    value(receipt, "paymentMethod"));

            addRow(
                    table,
                    "Payment Status",
                    value(receipt, "paymentStatus"));

            Object stripeId =
                    receipt.get("stripePaymentIntentId");

            if (stripeId != null
                    && !stripeId.toString().isBlank()) {

                addRow(
                        table,
                        "Stripe Reference",
                        stripeId.toString());
            }

            document.add(table);
            document.add(new Paragraph(" "));

            document.add(new Paragraph(
                    "Thank you for riding with RideGo!"));

            document.close();

            return output.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to generate receipt PDF", e);
        }
    }

    private void addRow(
            Table table,
            String label,
            String value) {

        table.addCell(label);

        table.addCell(
                value == null || value.isBlank()
                        ? "N/A"
                        : value);
    }

    private String value(
            Map<String, Object> receipt,
            String key) {

        Object value = receipt.get(key);

        return value == null
                ? "N/A"
                : value.toString();
    }

    private String formatAmount(
            Object amount,
            String currency) {

        if (amount == null) {
            return "N/A";
        }

        try {
            BigDecimal value =
                    new BigDecimal(amount.toString());

            String currencyText =
                    currency == null
                            || currency.isBlank()
                            || "N/A".equals(currency)
                            ? ""
                            : currency + " ";

            return currencyText + value.toPlainString();

        } catch (NumberFormatException e) {
            return amount.toString();
        }
    }

    private String formatDate(Object paidAt) {

        if (paidAt == null) {
            return "N/A";
        }

        if (paidAt instanceof LocalDateTime dateTime) {
            return dateTime.format(
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy, hh:mm a"));
        }

        return paidAt.toString();
    }
}
