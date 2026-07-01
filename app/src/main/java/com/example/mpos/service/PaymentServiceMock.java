package com.example.mpos.service;

import com.example.mpos.constants.PaymentConstants;

public class PaymentServiceMock {
    public PaymentResult pay(String method, long amount, long receivedCash) {
        if (amount <= 0) return PaymentResult.failed("Số tiền thanh toán không hợp lệ");
        if (PaymentConstants.METHOD_CASH.equals(method) && receivedCash < amount) {
            return PaymentResult.failed("Tiền khách đưa chưa đủ");
        }
        if (method != null && method.contains("FAIL")) {
            return PaymentResult.failed("Thanh toán mock thất bại");
        }
        String ref = "PAY-" + System.currentTimeMillis();
        return PaymentResult.success(ref);
    }

    public static final class PaymentResult {
        public final boolean success;
        public final String transactionRef;
        public final String message;

        private PaymentResult(boolean success, String transactionRef, String message) {
            this.success = success;
            this.transactionRef = transactionRef;
            this.message = message;
        }

        public static PaymentResult success(String transactionRef) {
            return new PaymentResult(true, transactionRef, "Thanh toán thành công");
        }

        public static PaymentResult failed(String message) {
            return new PaymentResult(false, null, message);
        }
    }
}
