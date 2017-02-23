import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Created by Clark on 2/4/2017.
 */
public class Util {
    public static ServerBootstrap createServerBootstrap(ChannelHandler channelInitializer) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        serverBootstrap.group(bossGroup, workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.childHandler(channelInitializer);

        return serverBootstrap;
    }


    public static Bootstrap createClientBootstrap(ChannelHandler channelHandler) {
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(channelHandler);

        return bootstrap;
    }


    public static TrustManagerFactory loadTrustStore (KeyStore keyStore) {
        FileInputStream fileInputStream = null;
        TrustManagerFactory trustManagerFactory = null;

        try {
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return trustManagerFactory;
    }


    public static void closeIgnoreExceptions(InputStream inputStream) {
        if (null != inputStream) {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
    }


    public static SslContext createClientSslContext (String filename) {
        SslContext sslContext = null;

        try {
            File file = new File(filename);
            sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(file)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sslContext;
    }


    public static SslContext createServerSslContext(String keyFilename, String keyPassword, String keyAlias,
                                                    String trustStoreFilename, String trustStorePassword, String trustStoreAlias) {
        SslContext sslContext = null;

        try {
            PrivateKey privateKey = getPrivateKey(keyFilename, keyPassword, keyAlias);
            X509Certificate certificate = getCertificate(trustStoreFilename, trustStorePassword, trustStoreAlias);

            sslContext = SslContextBuilder
                    .forServer(privateKey, certificate)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return sslContext;
    }


    public static PrivateKey getPrivateKey(String filename, String password, String alias) {
        KeyStore keyStore = getKeyStore(filename, password);
        PrivateKey privateKey = null;

        try {
            privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
        } catch (Exception e) {
            System.err.println("Exception trying to get private key");
            e.printStackTrace();
            System.exit(1);
        }

        return privateKey;
    }


    public static KeyStore getKeyStore(String filename, String passwordString) {
        KeyStore keyStore = null;
        FileInputStream fis = null;

        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = null;
            if (null != passwordString)
                password = passwordString.toCharArray();

            fis = new FileInputStream(filename);
            keyStore.load(fis, password);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            closeIgnoreExceptions(fis);
        }

        return keyStore;
    }

    public static X509Certificate getCertificate(String filename, String password, String alias) {
        X509Certificate certificate = null;
        KeyStore keyStore = getKeyStore(filename, password);

        try {
            certificate = (X509Certificate) keyStore.getCertificate(alias);
        } catch (Exception e) {
            System.err.println("Exception trying to get certificate");
            e.printStackTrace();
            System.exit(1);
        }

        return certificate;
    }

    public static SslContext createSimpleClientContext () {
        SslContext sslContext = null;

        try {
            sslContext = SslContextBuilder.forClient().build();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return sslContext;
    }
}
