An SSL test.

This is basically a "hello world" for SSL. It is intended to ensure that an SSL scheme is working.

To download it use

git clone git://github.com/ClarkHobbie/ssltest.git


You can use the truststore and keystore that come with the project or create your own.  To create a new truststore and keystore use the following commands (you must have openssl and keytool in your path):

`openssl req -x509 -newkey rsa:2048 -keyout ca-key.pem.txt -out ca-certificate.pem.txt -days 365 -nodes`

`keytool -import -keystore truststore -file ca-certificate.pem.txt -alias ca  -storepass whatever`

`keytool –keystore serverkeystore –genkey –alias server -keyalg rsa -storepass whatever`

`keytool –keystore serverkeystore -storepass whatever –certreq –alias server  –file server.csr`

`openssl x509 -req -CA ca-certificate.pem.txt -CAkey ca-key.pem.txt -in server.csr -out server.cer -days 365 –CAcreateserial`

`keytool -import -keystore serverkeystore -storepass whatever -file ca-certificate.pem.txt -alias ca`

`keytool -import -keystore serverkeystore -storepass whatever -file server.cer -alias server`

To run it, you need a keystore called "severkeystore" in the current directory that contains the server keys and certificates. You also need a file called "tuststore" that contains the certificate authority. Both files need to be in jks format.

To compile it use

mvn package

To run it use

java -cp target\ssl-test-1.0-SNAPSHOT.jar;lib\netty-all-4.1.6.Final.jar SSLTest server

and in a different window use

java -cp target\ssl-test-1.0-SNAPSHOT.jar;lib\netty-all-4.1.6.Final.jar SSLTest client

The client should pompt you with something like "localhost:6789> " when it gets a connection.

Entering anything should result in the following exception being printed out:

io.netty.handler.codec.DecoderException: javax.net.ssl.SSLKeyException: Invalid signature on ECDH server key exchange message
        at io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:442)
        at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:248)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:373)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:359)
        at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:351)
        at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1334)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:373)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:359)
        at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:926)
        at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:129)
        at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:651)
        at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:574)
        at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:488)
        at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:450)
        at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:873)
        at io.netty.util.concurrent.DefaultThreadFactory$DefaultRunnableDecorator.run(DefaultThreadFactory.java:144)
        at java.lang.Thread.run(Unknown Source)
Caused by: javax.net.ssl.SSLKeyException: Invalid signature on ECDH server key exchange message
        at sun.security.ssl.Handshaker.checkThrown(Unknown Source)
        at sun.security.ssl.SSLEngineImpl.checkTaskThrown(Unknown Source)
        at sun.security.ssl.SSLEngineImpl.readNetRecord(Unknown Source)
        at sun.security.ssl.SSLEngineImpl.unwrap(Unknown Source)
        at javax.net.ssl.SSLEngine.unwrap(Unknown Source)
        at io.netty.handler.ssl.SslHandler.unwrap(SslHandler.java:1097)
        at io.netty.handler.ssl.SslHandler.unwrap(SslHandler.java:968)
        at io.netty.handler.ssl.SslHandler.decode(SslHandler.java:902)
        at io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:411)
        ... 16 more
Caused by: javax.net.ssl.SSLKeyException: Invalid signature on ECDH server key exchange message
        at sun.security.ssl.HandshakeMessage$ECDH_ServerKeyExchange.<init>(Unknown Source)
        at sun.security.ssl.ClientHandshaker.processMessage(Unknown Source)
        at sun.security.ssl.Handshaker.processLoop(Unknown Source)
        at sun.security.ssl.Handshaker$1.run(Unknown Source)
        at sun.security.ssl.Handshaker$1.run(Unknown Source)
        at java.security.AccessController.doPrivileged(Native Method)
        at sun.security.ssl.Handshaker$DelegatedTask.run(Unknown Source)
        at io.netty.handler.ssl.SslHandler.runDelegatedTasks(SslHandler.java:1123)
        at io.netty.handler.ssl.SslHandler.unwrap(SslHandler.java:1008)
        ... 18 more

Use the strinq "quit" to end the client.

In a bit more accessible format, the commands to create a truststore and server keystore:

1) Create the local CA self-signed certificate and private key

    openssl req -x509 -newkey rsa:2048 -keyout ca-key.pem.txt -out ca-certificate.pem.txt -days 365 -nodes

2) Create the truststore

    keytool -import -keystore truststore -file ca-certificate.pem.txt -alias ca  -storepass whatever

3) Create the server keystore

    keytool –keystore serverkeystore –genkey –alias server -keyalg rsa -storepass whatever

4) Create a certificate signing request for the server

    keytool –keystore serverkeystore -storepass whatever –certreq –alias server –file server.csr

5) Sign the server CSR with the local CA

    openssl x509 -req -CA ca-certificate.pem.txt -CAkey ca-key.pem.txt -in server.csr -out server.cer -days 365 –CAcreateserial

6) Import the local CA to the server keystore

    keytool -import -keystore serverkeystore -storepass whatever -file ca-certificate.pem.txt -alias ca

7) Import the singed certificate to the sever kestore

    keytool -import -keystore serverkeystore -storepass whatever -file server.cer -alias server

