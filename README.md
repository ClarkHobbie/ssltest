An SSL test.

This is basically a "hello world" for SSL. It is intended to ensure that an SSL scheme is working.

To run it, you need a keystore called "severkeystore" in the current directory that contains the server keys and certificates. You also need a file called "tuststore" that contains the cetificate authority. Both files need to be in jks format.

To download it use

git clone git://github.com/ClarkHobbie/ssltest.git

To compile it use

mvn package

To run it use

java -cp target\ssl-test-1.0-SNAPSHOT.jar;netty-all-4.1.6.Final.jar Server

and in a different window use

java -cp target\ssl-test-1.0-SNAPSHOT.jar;netty-all-4.1.6.Final.jar Client

If it works, then the client should print out the message "It worked!" and terminate.