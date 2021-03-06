package org.zarroboogs.smartzpn.tunnel.shadowsocks;

import org.zarroboogs.smartzpn.tunnel.IEncryptor;
import org.zarroboogs.smartzpn.tunnel.Tunnel;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;


public class ShadowsocksTunnel extends Tunnel {

    private IEncryptor mEncryptor;
    private ShadowsocksConfig mConfig;
    private boolean m_TunnelEstablished;

    public ShadowsocksTunnel(ShadowsocksConfig config, Selector selector) throws Exception {
        super(config.ServerAddress, selector);
        if (config.Encryptor == null) {
            throw new Exception("Error: The Encryptor for ShadowsocksTunnel is null.");
        }
        mConfig = config;
        mEncryptor = config.Encryptor;
    }

    @Override
    protected void onConnected(ByteBuffer buffer) throws Exception {

        //构造socks5请求（跳过前3个字节）
        buffer.clear();
        buffer.put((byte) 0x03);//domain
        byte[] domainBytes = m_DestAddress.getHostName().getBytes();
        buffer.put((byte) domainBytes.length);//domain length;
        buffer.put(domainBytes);
        buffer.putShort((short) m_DestAddress.getPort());
        buffer.flip();

        mEncryptor.encrypt(buffer);
        if (write(buffer, true)) {
            m_TunnelEstablished = true;
            onTunnelEstablished();
        } else {
            m_TunnelEstablished = true;
            this.beginReceive();
        }
    }

    @Override
    protected boolean isTunnelEstablished() {
        return m_TunnelEstablished;
    }

    @Override
    protected void beforeSend(ByteBuffer buffer) throws Exception {
        mEncryptor.encrypt(buffer);
    }

    @Override
    protected void afterReceived(ByteBuffer buffer) throws Exception {
        mEncryptor.decrypt(buffer);
    }

    @Override
    protected void onDispose() {
        mConfig = null;
        mEncryptor = null;
    }

}