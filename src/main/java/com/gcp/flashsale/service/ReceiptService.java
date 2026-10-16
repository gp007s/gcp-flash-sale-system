package com.gcp.flashsale.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import com.gcp.flashsale.model.OrderFulfillmentDTO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

/** Builds the "Digital Order Receipt" PDF and stores it in the Cloud Storage bucket. */
@Service
public class ReceiptService {

    private final GcsService gcsService;

    public ReceiptService(GcsService gcsService) {
        this.gcsService = gcsService;
    }

    public void createAndStoreReceipt(OrderFulfillmentDTO order) throws IOException {
        gcsService.uploadPdf("receipts/" + order.getOrderId() + ".pdf", buildPdf(order));
    }

    byte[] buildPdf(OrderFulfillmentDTO order) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream out = new PDPageContentStream(document, page)) {
                out.beginText();
                out.setFont(PDType1Font.HELVETICA_BOLD, 18);
                out.newLineAtOffset(50, 750);
                out.showText("Flash Sale - Digital Order Receipt");
                out.setFont(PDType1Font.HELVETICA, 12);
                out.setLeading(20);
                out.newLine();
                out.newLine();
                out.showText("Order number: " + safe(order.getOrderId()));
                out.newLine();
                out.showText("Item: " + safe(order.getItemDetails()));
                out.newLine();
                out.showText("Quantity: " + order.getQuantity());
                out.newLine();
                out.showText("Customer: " + safe(order.getUserName()));
                out.newLine();
                out.showText("Amount paid: " + order.getPaymentAmount());
                out.newLine();
                out.showText("Date: " + Instant.now());
                out.endText();
            }

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            document.save(bytes);
            return bytes.toByteArray();
        }
    }

    // The built-in PDF fonts only support simple characters, so keep plain ASCII.
    private static String safe(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[^\\x20-\\x7E]", "?");
    }
}