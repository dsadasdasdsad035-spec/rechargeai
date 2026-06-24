package com.wildai.payment.channel;

public record ChannelRefundResult(
        boolean success,
        String channelRefundNo,
        String errorMessage
) {
    public static ChannelRefundResult ok(String channelRefundNo) {
        return new ChannelRefundResult(true, channelRefundNo, null);
    }

    public static ChannelRefundResult fail(String errorMessage) {
        return new ChannelRefundResult(false, null, errorMessage);
    }
}
