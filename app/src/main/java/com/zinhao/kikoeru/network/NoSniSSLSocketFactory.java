package com.zinhao.kikoeru.network;

import com.zinhao.kikoeru.App;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class NoSniSSLSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory delegate;

    public NoSniSSLSocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    private Socket patch(Socket socket) {
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            try {
                // 方式 A：彻底移除 SNI（针对大部分 Android 原生平台的原生底层实现）
                // 不同的 Android 版本底层实现不同，通常是 com.android.org.conscrypt.OpenSSLSocketImpl
                Class<?> cls = sslSocket.getClass();
//                try {
//                    // 尝试寻找 setUseSessionTickets 方法和 setHostname 方法
//                    java.lang.reflect.Method setHostnameMethod = cls.getMethod("setHostname", String.class);
//                    // 传入 null 可以让底层在握手时不发送 SNI 扩展
//                    setHostnameMethod.invoke(sslSocket, (String) null);
//                } catch (NoSuchMethodException e) {
//                    // 如果没有 setHostname，尝试直接清空主机名相关的配置
//                }

                // 方式 B：如果你不是想去掉 SNI，而是想“伪造”一个看似合法的 SNI，可以这样做：
                java.lang.reflect.Method setHostnameMethod = cls.getMethod("setHostname", String.class);
                setHostnameMethod.invoke(sslSocket, "asmr.one");


            } catch (Exception e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        }
        return socket;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
//        if(s!=null){
//            if(autoClose){
//                s.close();
//            }
//        }
//        InetAddress address = s.getInetAddress();
//        SSLSocket sslSocket = (SSLSocket) (getDefault().createSocket(address, port));
//        sslSocket.setEnabledProtocols(sslSocket.getSupportedProtocols());

//        return sslSocket;
        return patch(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return patch(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return patch(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return patch(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return patch(delegate.createSocket(address, port, localAddress, localPort));
    }
}
