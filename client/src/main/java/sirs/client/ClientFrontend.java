package sirs.client;

import io.grpc.StatusException;
import sirs.grpc.Contract.*;
import sirs.grpc.RemoteGrpc;
import io.grpc.ManagedChannel;
import com.google.protobuf.ByteString;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.*;
import java.security.cert.*;
import java.util.Base64;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLException;

public class ClientFrontend {
    final ManagedChannel channel;
    RemoteGrpc.RemoteBlockingStub stub;

    public ClientFrontend(String host, int port) throws SSLException {
        channel = NettyChannelBuilder.forAddress(host, port).sslContext(GrpcSslContexts.forClient().trustManager(new File("../TLS/ca-cert.pem")).build()).build();
        stub = RemoteGrpc.newBlockingStub(channel);
    }

    public void register(String username, ByteString certificate) {
        RegisterRequest request = RegisterRequest.newBuilder().setUsername(username).setCertificate(certificate).build();
        stub.register(request);
    }

    public void login(String username, PrivateKey privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {

        // Request number
        NumberRequest numberRequest = NumberRequest.newBuilder().setUsername(username).build();
        NumberResponse numberResponse = stub.getNumber(numberRequest);
        String number = numberResponse.getNumber();

        // Cipher number with private key
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] cipheredNumber = cipher.doFinal(number.getBytes("UTF8"));

        // Request login token
        TokenRequest tokenRequest = TokenRequest.newBuilder().setUsername(username).setNumber(ByteString.copyFrom(cipheredNumber)).build();
        TokenResponse tokenResponse = stub.getToken(tokenRequest);

        // Set token
        this.stub = this.stub.withCallCredentials(new AuthCreadentials(tokenResponse.getToken()));
    }

    public void upload(String filename, byte[] signature) throws FileNotFoundException, IOException {
        UploadRequest.Builder request = UploadRequest.newBuilder().setName(filename);
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        ByteString bytes = ByteString.readFrom(fis);
        ByteString sig = ByteString.copyFrom(signature);
        fis.close();

        request.setFile(bytes);
        request.setSignature(sig);

        stub.upload(request.build());
    }

    public boolean download(String filename) throws GeneralSecurityException, FileNotFoundException, IOException {
        DownloadRequest request = DownloadRequest.newBuilder().setName(filename).build();
        DownloadResponse response = stub.download(request);
        ByteString bytes = response.getFile();
        ByteString signature = response.getSignature();
        ByteString certificate = response.getCertificate();

        // Verify signature
        byte[] fileBytes = new byte[bytes.size()];
        bytes.copyTo(fileBytes, 0);

        byte[] sigBytes = new byte[signature.size()];
        signature.copyTo(sigBytes, 0);

        byte[] certBytes = new byte[certificate.size()];
        certificate.copyTo(certBytes, 0);

        if (verifySignature(fileBytes, certBytes, sigBytes)) {
            FileOutputStream fos = new FileOutputStream(filename);
            bytes.writeTo(fos);
            fos.close();
            return true;
        } else {
            return false;
        }
    }

    private boolean verifySignature(byte[] file, byte[] cert, byte[] signature) throws GeneralSecurityException, CertificateException {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(cert));

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(certificate);
        sig.update(file);

        return sig.verify(signature);
    }

    public void close() {
        if (this.channel != null) channel.shutdownNow();
    }
}