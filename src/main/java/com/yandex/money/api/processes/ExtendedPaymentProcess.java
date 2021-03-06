/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 NBCO Yandex.Money LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.yandex.money.api.processes;

import com.yandex.money.api.methods.BaseProcessPayment;
import com.yandex.money.api.methods.BaseRequestPayment;
import com.yandex.money.api.model.ExternalCard;
import com.yandex.money.api.model.MoneySource;
import com.yandex.money.api.model.Wallet;
import com.yandex.money.api.net.clients.ApiClient;

import static com.yandex.money.api.util.Common.checkNotNull;

/**
 * Combined payment process of {@link PaymentProcess} and {@link ExternalPaymentProcess}.
 *
 * @author Slava Yasevich (vyasevich@yamoney.ru)
 */
public final class ExtendedPaymentProcess implements IPaymentProcess {

    private final ApiClient client;
    private final PaymentProcess paymentProcess;
    private final ExternalPaymentProcess externalPaymentProcess;
    private final ExternalPaymentProcess.ParameterProvider parameterProvider;

    private PaymentContext paymentContext;
    private boolean mutablePaymentContext = true;

    /**
     * Constructor.
     *
     * @param client client to run the process on
     * @param parameterProvider parameter's provider
     */
    public ExtendedPaymentProcess(ApiClient client, ExternalPaymentProcess.ParameterProvider parameterProvider) {
        this.client = checkNotNull(client, "client");
        this.paymentProcess = new PaymentProcess(client, parameterProvider);
        this.externalPaymentProcess = new ExternalPaymentProcess(client, parameterProvider);
        this.parameterProvider = parameterProvider;
        invalidatePaymentContext();
    }

    @Override
    public boolean proceed() throws Exception {
        switchContextIfRequired();
        return paymentContext == PaymentContext.PAYMENT ? paymentProcess.proceed() :
                externalPaymentProcess.proceed();
    }

    @Override
    public boolean repeat() throws Exception {
        return paymentContext == PaymentContext.PAYMENT ? paymentProcess.repeat() :
                externalPaymentProcess.repeat();
    }

    @Override
    public void reset() {
        paymentProcess.reset();
        externalPaymentProcess.reset();
        invalidatePaymentContext();
    }

    @Override
    public BaseRequestPayment getRequestPayment() {
        return paymentContext == PaymentContext.PAYMENT ? paymentProcess.getRequestPayment() :
                externalPaymentProcess.getRequestPayment();
    }

    @Override
    public BaseProcessPayment getProcessPayment() {
        return paymentContext == PaymentContext.PAYMENT ? paymentProcess.getProcessPayment() :
                externalPaymentProcess.getProcessPayment();
    }

    /**
     * Initializes {@link ExtendedPaymentProcess} with fixed payment context. Must be called right
     * after instance creation, before calling {@link #proceed()}.
     *
     * @param paymentContext payment context
     * @return this instance for chain purposes
     * @throws IllegalStateException if called when process's state is not {@code CREATED}
     */
    public ExtendedPaymentProcess initWithFixedPaymentContext(PaymentContext paymentContext) {
        if (getState() != BasePaymentProcess.State.CREATED) {
            throw new IllegalStateException("you should call initWithFixedPaymentContext() after " +
                    "constructor only");
        }
        this.paymentContext = checkNotNull(paymentContext, "paymentContext");
        this.mutablePaymentContext = false;
        return this;
    }

    /**
     * @return saved state
     */
    public SavedState getSavedState() {
        return new SavedState(paymentProcess.getSavedState(),
                externalPaymentProcess.getSavedState(), paymentContext, mutablePaymentContext);
    }

    /**
     * Restores process to its saved state
     *
     * @param savedState saved state
     */
    public void restoreSavedState(SavedState savedState) {
        paymentProcess.restoreSavedState(checkNotNull(savedState, "saved state").paymentProcessSavedState);
        externalPaymentProcess.restoreSavedState(savedState.externalPaymentProcessSavedState);
        paymentContext = savedState.paymentContext;
        mutablePaymentContext = savedState.mutablePaymentContext;
    }

    /**
     * @see BasePaymentProcess#setAccessToken(String)
     */
    public void setAccessToken(String accessToken) {
        client.setAccessToken(accessToken);
        invalidatePaymentContext();
    }

    /**
     * @see ExternalPaymentProcess#setInstanceId(String)
     */
    public void setInstanceId(String instanceId) {
        externalPaymentProcess.setInstanceId(instanceId);
    }

    private void invalidatePaymentContext() {
        this.paymentContext = client.isAuthorized() ? PaymentContext.PAYMENT :
                PaymentContext.EXTERNAL_PAYMENT;
    }

    private void switchContextIfRequired() {
        if (getState() == BasePaymentProcess.State.STARTED && mutablePaymentContext) {
            MoneySource moneySource = checkNotNull(parameterProvider.getMoneySource(), "moneySource");

            if (paymentContext == PaymentContext.PAYMENT && moneySource instanceof ExternalCard) {
                paymentContext = PaymentContext.EXTERNAL_PAYMENT;
            } else if (paymentContext == PaymentContext.EXTERNAL_PAYMENT &&
                    moneySource instanceof Wallet) {
                paymentContext = PaymentContext.PAYMENT;
            }
        }
    }

    private BasePaymentProcess.State getState() {
        return paymentContext == PaymentContext.PAYMENT ? paymentProcess.getState() :
                externalPaymentProcess.getState();
    }

    /**
     * Payment context.
     */
    public enum PaymentContext {
        /**
         * {@link PaymentProcess}
         */
        PAYMENT,

        /**
         * {@link ExternalPaymentProcess}
         */
        EXTERNAL_PAYMENT
    }

    /**
     * Saved state of extended payment process.
     */
    public static final class SavedState {

        private final PaymentProcess.SavedState paymentProcessSavedState;
        private final ExternalPaymentProcess.SavedState externalPaymentProcessSavedState;
        private final PaymentContext paymentContext;
        private final boolean mutablePaymentContext;

        /**
         * Constructor.
         *
         * @param paymentProcessSavedState {@link PaymentProcess} saved state
         * @param externalPaymentProcessSavedState {@link ExtendedPaymentProcess} saved state
         * @param flags flags
         */
        public SavedState(PaymentProcess.SavedState paymentProcessSavedState,
                          ExternalPaymentProcess.SavedState externalPaymentProcessSavedState,
                          int flags) {

            this(paymentProcessSavedState, externalPaymentProcessSavedState, parseContext(flags),
                    parseMutablePaymentContext(flags));
        }

        private SavedState(PaymentProcess.SavedState paymentProcessSavedState,
                           ExternalPaymentProcess.SavedState externalPaymentProcessSavedState,
                           PaymentContext paymentContext, boolean mutablePaymentContext) {

            this.paymentProcessSavedState = paymentProcessSavedState;
            this.externalPaymentProcessSavedState = externalPaymentProcessSavedState;
            this.paymentContext = paymentContext;
            this.mutablePaymentContext = mutablePaymentContext;
        }

        /**
         * @return {@link PaymentProcess} saved state
         */
        public PaymentProcess.SavedState getPaymentProcessSavedState() {
            return paymentProcessSavedState;
        }

        /**
         * @return {@link ExtendedPaymentProcess} saved state
         */
        public ExternalPaymentProcess.SavedState getExternalPaymentProcessSavedState() {
            return externalPaymentProcessSavedState;
        }

        /**
         * @return flags for {@link ExtendedPaymentProcess}
         */
        public int getFlags() {
            return paymentContext.ordinal() + (mutablePaymentContext ? 10 : 0);
        }

        private static PaymentContext parseContext(int flags) {
            PaymentContext[] values = PaymentContext.values();
            int index = flags % 10;
            if (index >= values.length) {
                throw new IllegalArgumentException("invalid flags: " + flags);
            }
            return values[index];
        }

        private static boolean parseMutablePaymentContext(int flags) {
            int value = (flags / 10) % 10;
            if (value > 1) {
                throw new IllegalArgumentException("invalid flags: " + flags);
            }
            return value == 1;
        }
    }
}
