package com.example.mpos.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatOrderParser {
    private static final Pattern PHONE = Pattern.compile("(0|\\+84)[0-9\\s.-]{8,12}");
    private static final Pattern QUANTITY = Pattern.compile("(\\d+)\\s+([^,]+)");

    public ParsedChatOrder parse(String input) {
        String text = input == null ? "" : input.trim();
        ParsedChatOrder order = new ParsedChatOrder();
        Matcher phone = PHONE.matcher(text);
        if (phone.find()) order.phone = phone.group().replaceAll("[\\s.-]", "");
        String[] parts = text.split(",");
        if (parts.length > 0) order.customerName = parts[0].trim();
        String lower = text.toLowerCase();
        int addressIndex = indexOfAny(lower, "giao", "dia chi", "địa chỉ", "address");
        if (addressIndex >= 0) order.address = text.substring(addressIndex).trim();
        Matcher qty = QUANTITY.matcher(text);
        if (qty.find()) {
            try { order.quantity = Integer.parseInt(qty.group(1)); } catch (Exception ignored) { order.quantity = 1; }
            order.productKeyword = qty.group(2).trim();
        }
        if (order.quantity <= 0) order.quantity = 1;
        return order;
    }

    private int indexOfAny(String text, String... keywords) {
        int best = -1;
        for (String keyword : keywords) {
            int index = text.indexOf(keyword);
            if (index >= 0 && (best < 0 || index < best)) best = index;
        }
        return best;
    }

    public static final class ParsedChatOrder {
        public String customerName;
        public String phone;
        public String address;
        public String productKeyword;
        public int quantity = 1;
    }
}
