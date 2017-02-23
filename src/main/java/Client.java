import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

/**
 * Created by Clark on 2/4/2017.
 */
public class Client {
    public enum Modes {
        NoSSl,
        LocalCA,
        RemoteCA
    }

    public static class LocalChannelInitializer extends ChannelInitializer<SocketChannel> {
        private SslContext sslContext;
        private String message;

        public LocalChannelInitializer (SslContext sslContext, String message) {
            this.sslContext = sslContext;
            this.message = message;
        }

        public void initChannel (SocketChannel socketChannel) throws Exception{
            System.out.println("Got connection, initializing");

            if (null != sslContext) {
                SslHandler sslHandler = sslContext.newHandler(socketChannel.alloc());
                socketChannel.pipeline().addLast(sslHandler);
            }

            LocalHandler localHandler = new LocalHandler(message);
            socketChannel.pipeline().addLast(localHandler);
        }
    }

    public static class LocalHandler extends ChannelInboundHandlerAdapter {
        private String message;

        public LocalHandler (String message) {
            this.message = message;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println ("Sending " + message);

            ByteBuf byteBuf = Unpooled.directBuffer(256);
            ByteBufUtil.writeUtf8(byteBuf, message);
            ctx.writeAndFlush(byteBuf);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf byteBuf = (ByteBuf) msg;
            byte[] buffer = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(0, buffer);
            String s = new String(buffer);

            if (s.equals(message))
            {
                System.out.println ("It worked!");
                System.exit(0);
            }
        }
    }

    public void go (Modes mode, String host, int port) throws Exception {
        String certificateFilename = "ca-certificate.pem.txt";
        SslContext sslContext = null;

        if (mode == Modes.LocalCA) {
            sslContext = Util.createClientSslContext(certificateFilename);
        } else if (mode == Modes.RemoteCA) {
            sslContext = Util.createSimpleClientContext();
        }

        String message ="hello, world!";
        LocalChannelInitializer localChannelInitializer = new LocalChannelInitializer(sslContext, message);
        Bootstrap bootstrap = Util.createClientBootstrap(localChannelInitializer);

        System.out.println ("connecting to " + host + ":" + port);
        bootstrap.connect(host, port);
    }

    public static void main(String[] argv) {
        Client client = new Client();
        try {
            String host = "localhost";
            int port = 6789;
            Modes mode = Modes.LocalCA;

            if (argv.length > 0)
            {
                String s = argv[0];

                if ("nossl".equalsIgnoreCase(s))
                    mode = Modes.NoSSl;
                else if ("local".equalsIgnoreCase(s))
                    mode = Modes.LocalCA;
                else if ("remote".equalsIgnoreCase(s))
                    mode = Modes.RemoteCA;
            }

            if (argv.length > 1)
                host = argv[1];

            if (argv.length > 2)
                port = Integer.parseInt(argv[2]);

            client.go(mode, host, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
