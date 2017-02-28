An SSL test.

This is basically a "hello world" for SSL. It is intended to ensure that an SSL scheme is working.

To download it use

git clone git://github.com/ClarkHobbie/ssltest.git


You can use the truststore and keystore that come with the project or create your own.  To create a new truststore and keystore use the following commands (you must have openssl and keytool in your path):

'openssl req -x509 -newkey rsa:2048 -keyout ca-key.pem.txt -out ca-certificate.pem.txt -days 365 -nodes'
'keytool -import -keystore truststore -file ca-certificate.pem.txt -alias ca  -storepass whatever
'keytool –keystore serverkeystore –genkey –alias server -keyalg rsa -storepass whatever
'keytool –keystore serverkeystore -storepass whatever –certreq –alias server  –file server.csr
'openssl x509 -req -CA ca-certificate.pem.txt -CAkey ca-key.pem.txt -in server.csr -out server.cer -days 365 –CAcreateserial
'keytool -import -keystore serverkeystore -storepass whatever -file ca-certificate.pem.txt -alias ca
'keytool -import -keystore serverkeystore -storepass whatever -file server.cer -alias server

To run it, you need a keystore called "severkeystore" in the current directory that contains the server keys and certificates. You also need a file called "tuststore" that contains the certificate authority. Both files need to be in jks format.

To compile it use

mvn package

To run it use

java -cp target\ssl-test-1.0-SNAPSHOT.jar;lib\netty-all-4.1.6.Final.jar SSLTest server

and in a different window use

java -cp target\ssl-test-1.0-SNAPSHOT.jar;lib\netty-all-4.1.6.Final.jar SSLTest client

The client should pompt you with something like "localhost:6789> " when it gets a connection.  Use the strinq "quit" to end the client.