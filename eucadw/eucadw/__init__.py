# Copyright 2009-2012 Eucalyptus Systems, Inc.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
# Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
# additional information or have any questions.

import re
import sys
from optparse import OptionGroup

class EucaDatawarehouse():

    def get_db_option_group( self, parser ):
        dbgroup = OptionGroup( parser, "Database Connection Options",
                    "Options for connection to the datawarehouse")
        dbgroup.add_option("-H", "--database-host", dest="db_host", default="localhost", help="Database hostname")
        dbgroup.add_option("-P", "--database-port", dest="db_port", default="5432", help="Database port")
        dbgroup.add_option("-n", "--database-name", dest="db_name", default="eucalyptus_reporting", help="Database name")
        dbgroup.add_option("-u", "--database-user", dest="db_user", default="eucalyptus", help="Database username")
        dbgroup.add_option("-p", "--database-pass", dest="db_pass", default="", help="Database password")
        dbgroup.add_option("-s", "--database-use-ssl", dest="db_ssl", action="store_true", help="Database connections use SSL")
        return dbgroup

    def get_java_command( self, options, command_class ):
        #TODO determine / detect correct install location for JAR files
        command = [ 'java', '-cp', '/opt/datawarehouse/lib/*', 'com.eucalyptus.reporting.dw.commands.' + command_class ]
        command.append( '-dbh' )
        command.append( options.db_host )
        command.append( '-dbpo' )
        command.append( options.db_port )
        command.append( '-dbn' )
        command.append( options.db_name )
        command.append( '-dbu' )
        command.append( options.db_user )
        command.append( '-dbp' )
        command.append( options.db_pass )
        if options.db_ssl:
            command.append( '-dbs' )
        return command   


