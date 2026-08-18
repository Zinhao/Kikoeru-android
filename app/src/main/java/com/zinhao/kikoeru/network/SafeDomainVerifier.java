package com.zinhao.kikoeru.network;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class SafeDomainVerifier implements HostnameVerifier {
    private final String realHost;

    public SafeDomainVerifier(String realHost) {
        this.realHost = realHost;
    }

    @Override
    public boolean verify(String hostname, SSLSession session) {
        // 允许合法的内置校验：如果是我们期望的真实域名，直接返回 true
        // 注意：生产环境切勿直接 return true（会引入中间人攻击风险），必须校验是否与你的目标域名相符
        return hostname.equals(realHost) ||
                HttpsURLConnection.getDefaultHostnameVerifier().verify(realHost, session);
    }
}
