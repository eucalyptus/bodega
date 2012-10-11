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
package com.eucalyptus.reporting.dw.commands;

import static com.eucalyptus.reporting.dw.DataWarehouseSSLSocketFactory.CertificateNotTrustedException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.persistence.PersistenceException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.ejb.Ejb3Configuration;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.configuration.PropertyConfigurator;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.reporting.dw.DataWarehouseSSLSocketFactory;
import com.eucalyptus.reporting.export.ExportUtils;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Support class for commands
 */
abstract class CommandSupport {
  private static final String PROP_LOGGING_THRESHOLD = "com.eucalyptus.reporting.dw.logThreshold";
  private static final String PROP_EUCA_LOGGING_THRESHOLD = "euca.log.level";
  private static final String LOGGING_THRESHOLD_DEFAULT = Level.ERROR.toString();
  private static final String LOGGING_THRESHOLD_SILENT = Level.OFF.toString();
  private static final String LOGGING_THRESHOLD_DEBUG = Level.DEBUG.toString();
  private static final String RESOURCE_DATA_WAREHOUSE_PERSISTENCE_PROPERTIES =
      "com/eucalyptus/reporting/dw/datawarehouse_persistence.properties";
  private static final List<String> EUCA_LOG_LEVELS = ImmutableList.of(
      "EXHAUST", "EXTREME", "TRACE","DEBUG", "INFO", "WARN", "ERROR", "FATAL" );

  private static final String DEFAULT_CIPHER_STRINGS = "RSA+AES:+SHA:!EXPORT:!EXPORT1025:!MD5";
  private static final String DEFAULT_PROTOCOL = "TLS";

  private final Arguments arguments;
  private DatabaseConnectionInfo databaseConnectionInfo;

  public CommandSupport( final Arguments arguments ) {
    this.arguments = arguments;
  }

  protected final void run() {
    try {
      setupLogging();
      setupPersistenceContext();
      runCommand( arguments );
    } catch ( final Throwable e ) {
      handleCommandError( e );
    }
  }

  protected abstract void runCommand( Arguments arguments );

  protected void handleCommandError( final Throwable e ) {
    if ( e instanceof ConfigurationException ) {
      System.err.println( "Missing or invalid configuration." );
      System.err.println( e.getMessage() );
      return;
    }

    if ( Exceptions.isCausedBy( e, CertificateNotTrustedException.class ) ) {
      final CertificateNotTrustedException certificateNotTrustedException =
          Exceptions.findCause( e, CertificateNotTrustedException.class );
      System.err.println( "Database access failed due to untrusted server certificate." );
      System.err.println( "Issuer           : " + certificateNotTrustedException.getIssuer() );
      System.err.println( "Serial           : " + certificateNotTrustedException.getSerialNumber() );
      System.err.println( "Subject          : " + certificateNotTrustedException.getSubject() );
      System.err.println( "SHA-1 Fingerprint: " + certificateNotTrustedException.getSha1Fingerprint() );
      return;
    }

    if ( Exceptions.isCausedBy( e, PersistenceException.class ) &&
        Exceptions.isCausedBy( e, SQLException.class ) ) {
      final SQLException sqlException = Exceptions.findCause( e, SQLException.class );
      System.err.println( "Database access failed with the following details." );
      System.err.println( "SQLState  : " + sqlException.getSQLState() );
      System.err.println( "Error Code: " + sqlException.getErrorCode() );
      System.err.println( sqlException.getMessage() );
      return;
    }

    System.err.println( "Error processing command: " + e.getMessage() );
    Logger.getLogger( this.getClass() ).error( "Error processing command", e );
    System.exit(1);
  }

  protected DatabaseConnectionInfo getDatabaseConnectionInfo() {
    return databaseConnectionInfo;
  }

  protected static String format( final long timestamp ) {
    final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    return sdf.format( new Date( timestamp ) );
  }

  private void setupLogging() {
    final String threshold = arguments.getArgument( "log-threshold", null );
    final String log4JThreshold;
    if ( threshold == null ) {
      log4JThreshold = LOGGING_THRESHOLD_DEFAULT;
    } else if ( "debug".equalsIgnoreCase( threshold ) ) {
      log4JThreshold = LOGGING_THRESHOLD_DEBUG;
    } else if ( "silent".equalsIgnoreCase( threshold ) ) {
      log4JThreshold = LOGGING_THRESHOLD_SILENT;
    } else {
      log4JThreshold = threshold;
    }
    System.setProperty( PROP_LOGGING_THRESHOLD, log4JThreshold );
    final String eucaThreshold = EUCA_LOG_LEVELS.contains( log4JThreshold ) ?
        log4JThreshold : "FATAL";
    System.setProperty( PROP_EUCA_LOGGING_THRESHOLD, eucaThreshold );
  }

  private void setupPersistenceContext() {
    final Properties properties = new Properties();
    try {
      properties.load( CommandSupport.class.getClassLoader()
          .getResourceAsStream( RESOURCE_DATA_WAREHOUSE_PERSISTENCE_PROPERTIES ) );
    } catch (IOException e) {
      throw Exceptions.toUndeclared(e);
    }

    String dbPassword = System.getenv( "EUCADW_DB_PASS" );
    dbPassword = arguments.getArgument( "db-pass", dbPassword );
    if ( dbPassword == null && System.console() != null ) {
      char[] password = System.console().readPassword( "Database password: " );
      dbPassword = password != null ? new String( password ) : "";
    }
    if ( dbPassword == null ) {
      throw new ConfigurationException( "Database password not provided." );
    }

    databaseConnectionInfo = new DatabaseConnectionInfo(
        arguments.getArgument( "db-host", "localhost" ) ,
        arguments.getArgument( "db-port", "5432" ),
        arguments.getArgument( "db-name", "eucalyptus_reporting" ),
        arguments.getArgument( "db-user", "eucalyptus" ),
        dbPassword
    );

    if ( arguments.hasArgument( "db-ssl" ) ) {
      DataWarehouseSSLSocketFactory.initialize(
          arguments.getArgument( "db-ssl-provider", null ),
          arguments.getArgument( "db-ssl-protocol", DEFAULT_PROTOCOL ),
          arguments.getArgument( "db-ssl-ciphers", DEFAULT_CIPHER_STRINGS ),
          arguments.getArgument( "db-ssl-fingerprint", null ),
          !arguments.hasArgument( "db-ssl-skip-verify" )  );
    }

    properties.setProperty( "jdbc-0.proxool.driver-url",
        String.format( "jdbc:postgresql://%s:%s/%s%s",
            databaseConnectionInfo.getHost(),
            databaseConnectionInfo.getPort(),
            databaseConnectionInfo.getName(),
            arguments.hasArgument( "db-ssl" ) ?
                "?ssl=true&sslfactory=com.eucalyptus.reporting.dw.DataWarehouseSSLSocketFactory" :
                "" ) );
    properties.setProperty( "jdbc-0.user", databaseConnectionInfo.getUser() );
    properties.setProperty( "jdbc-0.password", databaseConnectionInfo.getPass() );

    try {
      PropertyConfigurator.configure( properties );
    } catch (ProxoolException e) {
      throw Exceptions.toUndeclared(e);
    }

    Ejb3Configuration config = new Ejb3Configuration();
    config.setProperties( properties );
    for ( Class<?> eventClass : ExportUtils.getPersistentClasses() ) {
      config.addAnnotatedClass( eventClass );
    }
    PersistenceContexts.registerPersistenceContext( "eucalyptus_reporting", config );
  }

  protected static ArgumentsBuilder argumentsBuilder() {
    return new ArgumentsBuilder();
  }

  static final class Arguments {
    private final CommandLine commandLine;

    private Arguments( final CommandLine commandLine ) {
      this.commandLine = commandLine;
    }

    String getArgument( final String name,
                        final String defaultValue ) {
      return Iterables.get( getArguments( name ), 0, defaultValue );
    }

    List<String> getArguments( final String name ) {
      return Lists.newArrayList(
          Objects.firstNonNull(commandLine.getOptionValues(name), new String[0]));
    }

    boolean hasArgument( final String name ) {
      return commandLine.hasOption( name );
    }
  }

  static final class DatabaseConnectionInfo {
    private final String host;
    private final String port;
    private final String name;
    private final String user;
    private final String pass;

    DatabaseConnectionInfo( final String host,
                            final String port,
                            final String name,
                            final String user,
                            final String pass ) {
      this.host = host;
      this.port = port;
      this.name = name;
      this.user = user;
      this.pass = pass;
    }

    public String getHost() {
      return host;
    }

    public String getPort() {
      return port;
    }

    public String getName() {
      return name;
    }

    public String getUser() {
      return user;
    }

    public String getPass() {
      return pass;
    }
  }

  static final class ArgumentsBuilder {
    private final Options options = new Options();

    private ArgumentsBuilder() {
      // DB arguments
      withArg( "dbh", "db-host", "Database hostname", false );
      withArg( "dbpo", "db-port", "Database port", false );
      withArg( "dbn",  "db-name", "Database name", false );
      withArg( "dbu",  "db-user", "Database username", false );
      withArg( "dbp",  "db-pass", "Database password", false );
      withFlag( "dbs", "db-ssl", "Database connection uses SSL" );
      withArg( "dbsf",  "db-ssl-fingerprint", "Database SSL trusted SHA-1 fingerprint", false );
      withArg( "dbsp",  "db-ssl-provider", "Database SSL provider", false );
      withArg( "dbst",  "db-ssl-protocol", "Database SSL protocol", false );
      withArg( "dbsc",  "db-ssl-ciphers", "Database SSL ciphers", false );
      withFlag( "dbsv", "db-ssl-skip-verify", "Database connection does not verify SSL certificate" );

      // Logging arguments
      withArg( "lt", "log-threshold", "Logging threshold", false );
    }

    ArgumentsBuilder withFlag( final String argument,
                               final String longName,
                               final String description ) {
      final Option option = new Option( argument, description );
      option.setLongOpt(longName);
      options.addOption(option);
      return this;
    }

    ArgumentsBuilder withArg( final String argument,
                              final String longName,
                              final String description,
                              final boolean required ) {
      final Option option = new Option( argument, description );
      option.setLongOpt(longName);
      option.setRequired(required);
      option.setArgs(1);
      option.setArgName(argument);
      options.addOption( option );
      return this;
    }

    Arguments forArgs( final String[] args ) throws IllegalArgumentException {
      try {
        return new Arguments( new GnuParser().parse( options, args ) );
      } catch (ParseException e) {
        throw new IllegalArgumentException( e );
      }
    }
  }

  static final class ConfigurationException extends RuntimeException {
    public ConfigurationException( final String message ) {
      super( message );
    }
  }
}
