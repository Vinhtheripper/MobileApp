package com.example.mpos.constants;

public final class PaymentConstants {
    private PaymentConstants() { }

    public static final String METHOD_CASH = "CASH";
    public static final String METHOD_QR = "QR";
    public static final String METHOD_MOMO = "MOMO";
    public static final String METHOD_VNPAY = "VNPAY";
    public static final String METHOD_CARD_NFC = "CARD_NFC";
    public static final String METHOD_SPLIT = "SPLIT";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_REFUNDED = "REFUNDED";
}
