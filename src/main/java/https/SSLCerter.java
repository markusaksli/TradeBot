package https;

import sun.security.x509.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SSLCerter {
    public static void main(String[] args) {
        String password = "123456";
        String keyPassword = "654321";
        try (
                FileOutputStream keyFos = new FileOutputStream("ks.jks"); FileOutputStream trustFos = new FileOutputStream("ts.jks");
        ) {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(4096);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            X509Certificate[] chain = {generateCertificate("CN=" + InetAddress.getLocalHost().getHostName() + "localhost, OU=Tradebot, O=lower third, C=EE", keyPair, 365 * 10, "SHA256withRSA")};

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setKeyEntry("main", keyPair.getPrivate(), keyPassword.toCharArray(), chain);
            keyStore.store(keyFos, password.toCharArray());

            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("main", chain[0]);
            keyStore.store(trustFos, password.toCharArray());

            //sun.security.tools.keytool.Main.main(new String[]{"-export", "-keystore", "ts.jks", "-alias", "main", "-file", "TradeBot.cer", "-storepass", password});
            sun.security.tools.keytool.Main.main(new String[]{"-importkeystore", "-srckeystore", "ts.jks", "-srcalias", "main", "-srcstorepass", password, "-destkeystore", "TradeBot.p12", "-deststorepass", password});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Create a self-signed X.509 Certificate
     *
     * @param dn        the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
     * @param pair      the KeyPair
     * @param days      how many days from now the Certificate is valid for
     * @param algorithm the signing algorithm, eg "SHA1withRSA"
     */
    static X509Certificate generateCertificate(String dn, KeyPair pair, int days, String algorithm)
            throws GeneralSecurityException, IOException {
        PrivateKey privkey = pair.getPrivate();
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date(from.getTime() + days * 86400000l);
        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name(dn);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER, owner);
        info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);

        // Update the algorith, and resign.
        algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);
        return cert;
    }
}
