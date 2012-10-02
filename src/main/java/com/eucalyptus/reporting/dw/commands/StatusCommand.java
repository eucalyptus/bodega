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

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import org.hibernate.CacheMode;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;
import com.eucalyptus.reporting.export.ExportUtils;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Iterables;

/**
 *  Data warehouse status command, invoked from Python wrapper.
 *
 *  <p>If the DB is available shows information on status. If not it tries to
 *  provide useful information on the state of the DB / connectivity.</p>
 */
public class StatusCommand extends CommandSupport {

  private static final Projection MIN = Projections.min( "timestampMs" );
  private static final Projection MAX = Projections.max( "timestampMs" );

  public StatusCommand(final String[] args) {
    super( argumentsBuilder().forArgs( args ) );
  }

  @Override
  protected void runCommand( final Arguments arguments ) {
    final DatabaseConnectionInfo dbInfo = getDatabaseConnectionInfo();

    long maxTime = Long.MIN_VALUE;
    long minTime = Long.MAX_VALUE;
    for ( final Class<? extends ReportingEventSupport> entityClass :
        Iterables.concat( ExportUtils.getEventClasses()/*, ExportUtils.getUsageClasses()*/ ) ) { //TODO:STEVE:uncomment when available
      final EntityTransaction transaction = Entities.get( entityClass );
      try {
        maxTime = Math.max( timestamp( entityClass, MAX, Long.MIN_VALUE ), maxTime );
        minTime = Math.min( timestamp( entityClass, MIN, Long.MAX_VALUE ), minTime );
      } finally {
        transaction.rollback();
      }
    }

    System.out.println( "Connected to database: " + dbInfo.getHost() + ":" +dbInfo.getPort()
        + "/" + dbInfo.getName() + " as " + dbInfo.getUser() );
    if ( minTime == Long.MAX_VALUE ) {
      System.out.println( "No data found." );
    } else {
      System.out.println( "Data present from " + format( minTime ) + " to " + format( maxTime ) );
    }
  }

  @Override
  protected void handleCommandError( final Throwable e ) {
    if ( e instanceof PersistenceException ) {
      if ( Exceptions.isCausedBy( e, SQLException.class ) ) {
        final SQLException sqlException = Exceptions.findCause( e, SQLException.class );
        System.out.println( "Database access failed with the following details." );
        System.out.println( "SQLState  : " + sqlException.getSQLState() );
        System.out.println( "Error Code: " + sqlException.getErrorCode() );
        System.out.println( sqlException.getMessage() );
        return;
      }
    }
    super.handleCommandError( e );
  }

  private long timestamp( final Class<? extends ReportingEventSupport> entityClass,
                          final Projection projection,
                          final long defaultValue ) {
    final Number value = (Number) Entities.createCriteria( entityClass )
        .setReadOnly(true)
        .setCacheable(false)
        .setCacheMode(CacheMode.IGNORE)
        .setProjection(projection)
        .uniqueResult();
    return value == null ? defaultValue : value.longValue();
  }

  private String format( final long timestamp ) {
    final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    return sdf.format( new Date( timestamp ) );
  }

  public static void main( final String[] args ) {
    new StatusCommand( args ).run();
  }
}
