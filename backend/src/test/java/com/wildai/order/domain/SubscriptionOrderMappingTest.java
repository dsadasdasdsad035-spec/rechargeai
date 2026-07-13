package com.wildai.order.domain;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionOrderMappingTest {

    @Test
    void accountTokenUsesTextColumnForLongEncryptedSessionTokens() throws Exception {
        Column column = SubscriptionOrder.class
                .getDeclaredField("accountTokenEnc")
                .getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo("account_token_enc");
        assertThat(column.columnDefinition()).isEqualToIgnoringCase("TEXT");
    }
}
