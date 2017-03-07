import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Created by Clark on 2/27/2017.
 */
public class SSLTest {
    public static class ServerChannelInitializer extends ChannelInitializer<NioSocketChannel> {
        private SslContext sslContext;

        public ServerChannelInitializer (SslContext sslContext) {
            this.sslContext = sslContext;
        }

        public void initChannel (NioSocketChannel serverSocketChannel) {
            if (null != sslContext) {
                SslHandler sslHandler = sslContext.newHandler(serverSocketChannel.alloc());
                serverSocketChannel.pipeline().addLast(sslHandler);
            }

            EchoHandler echoHandler = new EchoHandler();
            serverSocketChannel.pipeline().addLast(echoHandler);
        }
    }

    public static class UserInput {
        private static UserInput ourInstance;

        private String prompt;
        private BufferedReader bufferedReader;

        public static synchronized void initializeClass (String prompt) {
            if (null == ourInstance) {
                ourInstance = new UserInput (prompt);
            }
        }

        public static UserInput getInstance () {
            return ourInstance;
        }

        private UserInput (String prompt) {
            this.prompt = prompt;

            InputStreamReader inputStreamReader = new InputStreamReader(System.in);
            this.bufferedReader = new BufferedReader(inputStreamReader);
        }

        public String getLine () throws IOException {
            System.out.print (prompt);
            return bufferedReader.readLine();
        }
    }

    public static class ClientInitializer extends ChannelInitializer<SocketChannel> {
        private SslContext sslContext;

        public ClientInitializer (SslContext sslContext) {
            this.sslContext = sslContext;
        }

        public void initChannel (SocketChannel socketChannel) {
            if (null != sslContext) {
                SslHandler sslHandler = sslContext.newHandler(socketChannel.alloc());
                socketChannel.pipeline().addLast(sslHandler);
            }

            ClientChannelHandler clientChannelHandler = new ClientChannelHandler();
            socketChannel.pipeline().addLast(clientChannelHandler);
        }
    }

    public static class ClientChannelHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            String input = UserInput.getInstance().getLine();

            ByteBuf byteBuf = Unpooled.directBuffer(256);
            ByteBufUtil.writeUtf8(byteBuf, input);
            ctx.writeAndFlush(byteBuf);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf byteBuf = (ByteBuf) msg;
            byte[] buffer = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(0, buffer);
            String s = new String(buffer);

            System.out.println (s);

            s = UserInput.getInstance().getLine();
            if (s.equalsIgnoreCase("quit") || s.equalsIgnoreCase("bye")) {
                System.out.println ("quiting");
                System.exit(0);
            }

            byteBuf = Unpooled.directBuffer(256);
            ByteBufUtil.writeUtf8(byteBuf, s);
            ctx.writeAndFlush(byteBuf);
        }

        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static class EchoHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf byteBuf = (ByteBuf) msg;
            byte[] buffer = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(0, buffer);
            String s = new String(buffer);

            System.out.println("got " + s);

            ctx.writeAndFlush(msg);
        }
    }

    public static class CommandLine {
        private String[] argv;
        private int argIndex = 0;
        private String mode = "server";
        private boolean useTls = true;
        private String host = "localhost";
        private int port = 6789;

        public String[] getArgv () {
            return argv;
        }

        public String getArg () {
            if (argIndex >= argv.length)
                return null;

            return argv[argIndex];
        }

        public void advance () {
            argIndex++;
        }

        public String getMode () {
            return mode;
        }

        public void setMode (String mode) {
            this.mode = mode;
        }

        public boolean useTls () {
            return useTls;
        }

        public void setUseTls (boolean useTls) {
            this.useTls = useTls;
        }

        public String getHost () {
            return host;
        }

        public void setHost (String host) {
            this.host = host;
        }

        public int getPort () {
            return port;
        }

        public void setPort (int port) {
            this.port = port;
        }

        public CommandLine (String[] argv) {
            this.argv = argv;
            parse();
        }

        public void parse () {
            if (argv.length < 1)
                return;

            if (null != getArg() && getArg().equalsIgnoreCase("nossl")) {
                System.out.println ("Plaintext mode");
                setUseTls(false);
                advance();
            }

            if (null != getArg()) {
                setMode(getArg());
                advance();
            }

            if (null != getArg()) {
                setHost(getArg());
                advance();
            }

            if (null != getArg()) {
                int temp = Integer.parseInt(getArg());
                setPort(temp);
                advance();
            }
        }
    }

    private CommandLine commandLine;

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public SSLTest (CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    public static void closeIgnoreExceptions (Reader reader)
    {
        if (null != reader) {
            try {
                reader.close();
            } catch (IOException e) {}
        }
    }

    public static void closeIgnoreExceptions (InputStream inputStream) {
        if (null != inputStream) {
            try {
                inputStream.close();
            } catch (IOException e) {}
        }
    }

    public static void closeIfNonNull (PrintWriter printWriter) {
        if (null != printWriter) {
            printWriter.close();
        }
    }

    public static KeyStore getKeyStore (String filename, String password) {
        KeyStore keyStore = null;
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(filename);
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load (fileInputStream, password.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            closeIgnoreExceptions(fileInputStream);
        }

        return keyStore;
    }

    public static PrivateKey getPrivateKey (String filename, String password, String alias) {
        PrivateKey privateKey = null;
        FileInputStream fileInputStream = null;

        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            fileInputStream = new FileInputStream(filename);
            keyStore.load(fileInputStream, password.toCharArray());
            privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return privateKey;
    }


    public void server () {
        try {
            SslContext sslContext = null;

            if (getCommandLine().useTls()) {
                String trustStoreFilename = "truststore";
                String trustStorePassword = "whatever";
                String trustStoreAlias = "ca";

                String keyStoreFilename = "serverkeystore";
                String keyStorePassword = "whatever";
                String keyStoreAlias = "server";

                X509Certificate certificate = getCertificate(trustStoreFilename, trustStorePassword, trustStoreAlias);
                PrivateKey privateKey = getPrivateKey(keyStoreFilename, keyStorePassword, keyStoreAlias);
                sslContext = SslContextBuilder
                        .forServer(privateKey, certificate)
                        .build();
            }

            ServerChannelInitializer serverChannelInitializer = new ServerChannelInitializer(sslContext);

            NioEventLoopGroup workerGroup = new NioEventLoopGroup();
            NioEventLoopGroup bossGroup = new NioEventLoopGroup();

            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.childHandler(serverChannelInitializer);
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);

            System.out.println ("listening on port " + getCommandLine().getPort());

            serverBootstrap.bind(getCommandLine().getPort()).sync();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static X509Certificate getCertificate (String filename, String password, String alias) {
        KeyStore keyStore = null;
        FileInputStream fileInputStream = null;
        Certificate certificate = null;

        try {
            fileInputStream = new FileInputStream(filename);
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(fileInputStream, password.toCharArray());
            certificate = keyStore.getCertificate(alias);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return (X509Certificate) certificate;
    }

    public static TrustManagerFactory getTrustManagerFactory (String filename, String password) {
        TrustManagerFactory trustManagerFactory = null;

        try {
            KeyStore keyStore = getKeyStore(filename, password);
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return trustManagerFactory;
    }

    public void client () {
        try {
            String trustStoreFilename = "truststore";
            String trustStorePassword = "whatever";

            SslContext sslContext = null;

            if (getCommandLine().useTls()) {
                TrustManagerFactory trustManagerFactory = getTrustManagerFactory(trustStoreFilename, trustStorePassword);
                sslContext = SslContextBuilder
                        .forClient()
                        .trustManager(trustManagerFactory)
                        .build();
            }

            NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();

            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.channel(NioSocketChannel.class);
            clientBootstrap.group(nioEventLoopGroup);
            clientBootstrap.handler(new ClientInitializer(sslContext));
            clientBootstrap.connect(getCommandLine().getHost(), getCommandLine().getPort());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main (String[] argv) {
        System.setProperty("javax.net.ssl.trustStore", "wrong");
        System.setProperty("javax.net.ssl.trustStorePassword", "whatever");

        CommandLine commandLine = new CommandLine(argv);
        SSLTest sslTest = new SSLTest(commandLine);

        String prompt = commandLine.getHost() + ":" + commandLine.getPort() + "> ";
        UserInput.initializeClass(prompt);

        if (commandLine.getMode().equalsIgnoreCase("server"))
            sslTest.server();
        else if (commandLine.getMode().equalsIgnoreCase("client"))
            sslTest.client();
        else {
            System.err.println ("unknown mode: " + commandLine.getMode());
        }
    }
}
