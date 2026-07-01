package com.example.mpos.service;

public class ShippingServiceMock {
    public ShipmentQuote quote(String provider) {
        long base = "GHN".equals(provider) ? 25000 : "GHTK".equals(provider) ? 22000 : 28000;
        return new ShipmentQuote(provider, base, "2-3 ngày");
    }

    public String createTrackingCode(String provider) {
        return provider + "-" + System.currentTimeMillis();
    }

    public static final class ShipmentQuote {
        public final String provider;
        public final long fee;
        public final String eta;

        public ShipmentQuote(String provider, long fee, String eta) {
            this.provider = provider;
            this.fee = fee;
            this.eta = eta;
        }
    }
}
