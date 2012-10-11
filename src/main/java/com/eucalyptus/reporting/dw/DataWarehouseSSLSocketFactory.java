/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.reporting.dw;

import static com.eucalyptus.crypto.util.SslUtils.getEnabledCipherSuites;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.toArray;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Logger;
import com.eucalyptus.crypto.Digest;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

/**
 * SSLSocketFactory used by the data warehouse for database connections.
 */
public class DataWarehouseSSLSocketFactory extends SSLSocketFactory {

  private static final Logger logger = Logger.getLogger(DataWarehouseSSLSocketFactory.class);

  private final Supplier<SSLSocketFactory> delegate;
  private final List<String> cipherSuites;

  private static String sslProvider;
  private static String sslProtocol;
  private static String cipherStrings;
  private static String certificateFingerprint;
  private static boolean verifyCertificate;

  public static void initialize( final String sslProvider,
                                 final String sslProtocol,
                                 final String cipherStrings,
                                 final String certificateFingerprint,
                                 final boolean verifyCertificate ) {
    DataWarehouseSSLSocketFactory.sslProvider = sslProvider;
    DataWarehouseSSLSocketFactory.sslProtocol = sslProtocol;
    DataWarehouseSSLSocketFactory.cipherStrings = cipherStrings;
    DataWarehouseSSLSocketFactory.certificateFingerprint = certificateFingerprint;
    DataWarehouseSSLSocketFactory.verifyCertificate = verifyCertificate;
  }

  public DataWarehouseSSLSocketFactory() {
    this.delegate = Suppliers.ofInstance(
        buildDelegate( sslProvider, sslProtocol, certificateFingerprint, verifyCertificate )
    );
    this.cipherSuites =
        ImmutableList.copyOf( getEnabledCipherSuites( cipherStrings, getSupportedCipherSuites() ) );
    logger.info("Using cipher suites: " + cipherSuites);
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return delegate().getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return delegate().getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket( final Socket socket,
                              final String host,
                              final int port,
                              final boolean autoClose ) throws IOException {
    return notifyCreated(delegate().createSocket(socket, host, port, autoClose));
  }

  @Override
  public Socket createSocket( final String host,
                              final int port ) throws IOException {
    return notifyCreated(delegate().createSocket(host, port));
  }

  @Override
  public Socket createSocket( final String host,
                              final int port,
                              final InetAddress localHost,
                              final int localPort ) throws IOException {
    return notifyCreated(delegate().createSocket(host, port, localHost, localPort));
  }

  @Override
  public Socket createSocket( final InetAddress host,
                              final int port ) throws IOException {
    return notifyCreated(delegate().createSocket(host, port));
  }

  @Override
  public Socket createSocket( final InetAddress address,
                              final int port,
                              final InetAddress localAddress,
                              final int localPort ) throws IOException {
    return notifyCreated(delegate().createSocket(address, port, localAddress, localPort));
  }

  private Socket notifyCreated( final Socket socket ) {
    if ( socket instanceof SSLSocket ) {
      final SSLSocket sslSocket = (SSLSocket) socket;
      sslSocket.setEnabledCipherSuites( toArray(cipherSuites,String.class) );
    }
    return socket;
  }

  private SSLSocketFactory delegate() {
    return delegate.get();
  }

  private static String fingerprint( final X509Certificate certificate ) throws CertificateEncodingException {
    return hex( Digest.SHA1.get().digest( certificate.getEncoded() ) );
  }

  private static String hex( final byte[] bytes ) {
    final StringBuilder sb = new StringBuilder( );
    for ( final byte b : bytes ) {
      sb.append( String.format( "%02X:", b ) );
    }
    return sb.substring( 0, sb.length( ) - 1 ).toLowerCase( );
  }

  private static SSLSocketFactory buildDelegate( final String provider,
                                                 final String protocol,
                                                 final String fingerprint,
                                                 final boolean verify ) {
    final SSLContext sslContext;
    try {
      sslContext = provider!=null ?
          SSLContext.getInstance( protocol, provider ) :
          SSLContext.getInstance( protocol );
      final TrustManager trustManager =
          new FingerprintClientX509TrustManager( fingerprint , verify );
      sslContext.init( null, new TrustManager[]{ trustManager }, null );
      return sslContext.getSocketFactory();
    } catch ( NoSuchAlgorithmException e ) {
      throw propagate(e);
    } catch (KeyManagementException e) {
      throw propagate(e);
    } catch (NoSuchProviderException e) {
      throw propagate(e);
    }
  }

  public static final class CertificateNotTrustedException extends CertificateException {
    private final String issuer;
    private final String serialNumber;
    private final String subject;
    private final String sha1Fingerprint;

    public CertificateNotTrustedException( final String issuer,
                                           final String serialNumber,
                                           final String subject,
                                           final String sha1Fingerprint ) {
      super( "Certificate not trusted issuer '" + issuer + "', serial number " + serialNumber );
      this.issuer = issuer;
      this.serialNumber = serialNumber;
      this.subject = subject;
      this.sha1Fingerprint = sha1Fingerprint;
    }

    public String getIssuer() {
      return issuer;
    }

    public String getSerialNumber() {
      return serialNumber;
    }

    public String getSubject() {
      return subject;
    }

    public String getSha1Fingerprint() {
      return sha1Fingerprint;
    }
  }

  private static class FingerprintClientX509TrustManager implements X509TrustManager {
    private final String certificateSha1Fingerprint;
    private final boolean checkValidity;

    public FingerprintClientX509TrustManager( final String certificateSha1Fingerprint,
                                              final boolean checkValidity ) {
      this.certificateSha1Fingerprint = certificateSha1Fingerprint == null ?
          null : certificateSha1Fingerprint.toLowerCase();
      this.checkValidity = checkValidity;
    }

    @Override
    public void checkClientTrusted( final X509Certificate[] chain,
                                    final String authType ) throws CertificateException {
      throw new IllegalStateException("This trust manager is for client use only.");
    }

    @Override
    public void checkServerTrusted( final X509Certificate[] chain,
                                    final String authType ) throws CertificateException {
      if ( chain == null || chain.length < 1 ) {
        throw new IllegalArgumentException("Certificate chain invalid");
      } else if( certificateSha1Fingerprint==null ||
          !fingerprint(chain[0]).startsWith( certificateSha1Fingerprint ) ) {
        throw new CertificateNotTrustedException(
            chain[0].getIssuerX500Principal().getName(),
            hex( chain[0].getSerialNumber().toByteArray() ),
            chain[0].getSubjectX500Principal().getName(),
            fingerprint(chain[0]));
      }

      if ( checkValidity ){
        chain[0].checkValidity();
      }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }
}

