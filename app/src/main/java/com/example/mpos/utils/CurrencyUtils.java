package com.example.mpos.utils;

import java.text.NumberFormat;
import java.util.Locale;
public final class CurrencyUtils {
    private CurrencyUtils() { }
    public static String vnd(long value) { return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(value) + " ₫"; }
}
