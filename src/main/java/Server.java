import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;


public class Server {
    public enum Modes {
        NoSsl,
        LocalCA,
        RemoteCA
    }

    public static class LocalChannelInitializer extends ChannelInitializer<NioSocketChannel> {
        private SslContext sslContext;

        public LocalChannelInitializer (SslContext sslContext) {
            this.sslContext = sslContext;
        }

        protected void initChannel(NioSocketChannel serverSocketChannel) throws Exception {
            System.out.println("Got connection from " + serverSocketChannel.remoteAddress());

            if (null != sslContext) {
                SslHandler sslHandler = sslContext.newHandler(serverSocketChannel.alloc());
                serverSocketChannel.pipeline().addLast(sslHandler);
            }

            EchoHandler echoHandler = new EchoHandler();
            serverSocketChannel.pipeline().addLast(echoHandler);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
            System.exit(1);
        }
    }

    public static void main (String[] argv)
    {
        Modes mode = Modes.LocalCA;
        if (argv.length > 0) {
            String s = argv[0];

            if ("local".equalsIgnoreCase(s))
                mode = Modes.LocalCA;
            else if ("remote".equalsIgnoreCase(s))
                mode = Modes.RemoteCA;
            else if ("nossl".equalsIgnoreCase(s))
                mode = Modes.NoSsl;
        }

        Server server = new Server();
        server.go(mode);
    }

    public void go (Modes mode) {
        String trustStoreFilename = "truststore";
        String trustStorePassword = "whatever";
        String trustStoreAlias = "ca";

        String keyStoreFilename = "serverkeystore";
        String keyStorePassword = "whatever";
        String keyStoreAlias = "server";

        int port = 6789;

        SslContext sslContext = null;

        if (mode == Modes.LocalCA)
        {
            sslContext = Util.createServerSslContext(keyStoreFilename, keyStorePassword, keyStoreAlias, trustStoreFilename, trustStorePassword, trustStoreAlias);
        }

        LocalChannelInitializer localChannelInitializer = new LocalChannelInitializer(sslContext);
        ServerBootstrap serverBootstrap = Util.createServerBootstrap(localChannelInitializer);

        System.out.println ("Listening on " + port);

        try {
            serverBootstrap.bind(port).sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
