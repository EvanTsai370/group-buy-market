package org.example.domain.model.user.valueobject;

/**
 * OAuth 提供商
 */
public enum OAuthProvider {

    WECHAT("微信"),
    QQ("QQ"),
    ALIPAY("支付宝");

    private final String desc;

    OAuthProvider(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
