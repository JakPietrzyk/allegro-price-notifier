package com.priceprocessor.services;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    private static final String SCRAPER_ERRORS_METRIC = "scraper.errors.total";
    private static final String AUTH_LOGIN_METRIC = "auth.login";
    private static final String AUTH_REGISTER_METRIC = "auth.register";
    private static final String QUEUE_MAIL_SENT_METRIC = "queue.mail.sent";
    private static final String PRODUCT_PRICE_UPDATE = "product.price.update";

    private static final String TAG_KEY_STATUS = "status";
    private static final String TAG_KEY_REASON = "reason";
    private static final String TAG_KEY_ACTION = "action";

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";
    private static final String REASON_NONE = "none";
    private static final String ACTION_ADDED = "added";
    private static final String ACTION_DELETED = "deleted";

    public void incrementScraperError(String reason) {
        Counter.builder(SCRAPER_ERRORS_METRIC)
                .tag(TAG_KEY_STATUS, STATUS_FAILURE)
                .tag(TAG_KEY_REASON, reason)
                .register(meterRegistry)
                .increment();
    }

    public void incrementLoginSuccess() {
        incrementMetric(STATUS_SUCCESS, REASON_NONE, AUTH_LOGIN_METRIC);
    }

    public void incrementLoginFailure(String reason) {
        incrementMetric(STATUS_FAILURE, reason, AUTH_LOGIN_METRIC);
    }

    public void incrementRegisterSuccess() {
        incrementMetric(STATUS_SUCCESS, REASON_NONE, AUTH_REGISTER_METRIC);
    }

    public void incrementRegisterFailure(String reason) {
        incrementMetric(STATUS_FAILURE, reason, AUTH_REGISTER_METRIC);
    }

    public void incrementMailQueueSuccess() {
        incrementMetric(STATUS_SUCCESS, REASON_NONE, QUEUE_MAIL_SENT_METRIC);
    }

    public void incrementMailQueueFailure(String reason) {
        incrementMetric(STATUS_FAILURE, reason, QUEUE_MAIL_SENT_METRIC);
    }

    public void incrementProductPriceUpdateSuccess() {
        incrementMetric(STATUS_SUCCESS, REASON_NONE, PRODUCT_PRICE_UPDATE);
    }

    public void incrementProductPriceUpdateFailure(String reason) {
        incrementMetric(STATUS_FAILURE, reason, PRODUCT_PRICE_UPDATE);
    }

    private void incrementMetric(String status, String reason, String metricName) {
        Counter.builder(metricName)
                .tag(TAG_KEY_STATUS, status)
                .tag(TAG_KEY_REASON, reason)
                .register(meterRegistry)
                .increment();
    }
}