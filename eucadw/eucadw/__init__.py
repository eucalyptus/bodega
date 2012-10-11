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
import optparse
import os
import subprocess
from ConfigParser import ConfigParser
from optparse import OptionGroup, OptionParser

class EucaDatawarehouse():

    options = []

    config_defaults = {
        'db_host': 'localhost',
        'db_port': '5432',
        'db_name': 'eucalyptus_reporting',
        'db_user': 'eucalyptus',
        'db_pass': None,
        'db_ssl': False
        }

    def get_db_option_group( self, parser ):
        dbgroup = OptionGroup( parser, "Database Connection Options",
                    "Options for connection to the datawarehouse")
        dbgroup.add_option("-H", "--database-host", dest="db_host", help="Database hostname")
        dbgroup.add_option("-P", "--database-port", dest="db_port", help="Database port")
        dbgroup.add_option("-n", "--database-name", dest="db_name", help="Database name")
        dbgroup.add_option("-u", "--database-user", dest="db_user", help="Database username")
        dbgroup.add_option("-p", "--database-pass", dest="db_pass", help="Database password")
        dbgroup.add_option("-l", "--database-use-ssl", dest="db_ssl", action="store_true", help="Database connections use SSL")
        dbgroup.add_option("-i", "--database-ssl-fingerprint", dest="db_ssl_fingerprint", help="Database connection SSL certificate SHA-1 fingerprint")
        dbgroup.add_option("--database-ssl-provider", dest="db_ssl_provider", help=optparse.SUPPRESS_HELP)
        dbgroup.add_option("--database-ssl-protocol", dest="db_ssl_protocol", help=optparse.SUPPRESS_HELP)
        dbgroup.add_option("--database-ssl-ciphers", dest="db_ssl_ciphers", help=optparse.SUPPRESS_HELP)
        dbgroup.add_option("--database-ssl-skip-verify", dest="db_ssl_skip_verify", action="store_true", help=optparse.SUPPRESS_HELP)
        return dbgroup

    def get_common_option_group( self, parser ):
        common_group = OptionGroup( parser, "Common Options",
            "Options applicable to any command")
        common_group.add_option("-C", "--config-file", dest="config_file", help="Load configuration from the specified file")
        common_group.add_option("-I", "--config-ignore", dest="config_ignore", action="store_true", help="Ignore configuration files unless explicitly specified")
        common_group.add_option("-D", "--debug", dest="logging_debug", action="store_true", help="Enable debug logging")
        common_group.add_option("-E", "--errors", dest="logging_error", action="store_true", help="Enable error logging")
        common_group.add_option("-J", "--jvm", dest="jvm_options", action="append", help="Options passed to Java VM")
        return common_group

    def add_config_defaults( self, options ):
        config = ConfigParser()
        config_files = []
        if options.config_file is not None:
            config_files.append( options.config_file )
        if not options.config_ignore:
            config_files = config_files + [ 'eucadw.cfg', os.path.expanduser('~/.eucadw/eucadw.cfg'), '/etc/eucadw/eucadw.cfg' ]
        config.read( config_files )
        if config.has_section( 'database' ):
            for ( name, value ) in config.items( 'database' ):
                option_name = 'db_' + name
                if getattr( options, option_name, None ) is None:
                    options._update_careful( { option_name: value } )
        for name, value in self.config_defaults.iteritems():
            if getattr( options, name, None ) is None:
                options._update_careful( { name: value } )
        return options

    def get_java_command( self, options, command_class ):
        command = [ 'java' ]
        if options.jvm_options:
            command += options.jvm_options
        command += [ '-cp', '/usr/share/java/eucadw/*', 'com.eucalyptus.reporting.dw.commands.' + command_class ]
        command.append( '-dbh' )
        command.append( options.db_host )
        command.append( '-dbpo' )
        command.append( options.db_port )
        command.append( '-dbn' )
        command.append( options.db_name )
        command.append( '-dbu' )
        command.append( options.db_user )
        if options.db_ssl:
            command.append( '-dbs' )
        if options.db_ssl_fingerprint is not None:
            command.append( '-dbsf' )
            command.append( options.db_ssl_fingerprint )
        if options.db_ssl_provider:
            command.append( '-dbsp' )
            command.append( options.db_ssl_provider )
        if options.db_ssl_protocol:
            command.append( '-dbst' )
            command.append( options.db_ssl_protocol )
        if options.db_ssl_ciphers:
            command.append( '-dbsc' )
            command.append( options.db_ssl_ciphers )
        if options.db_ssl_skip_verify:
            command.append( '-dbsv' )
        if options.logging_debug:
            command.append( '-lt' )
            command.append( 'debug' )
        elif not options.logging_error:
            command.append( '-lt' )
            command.append( 'silent' )
        return command

    def run_java_command( self, options, command_class, command_args ):
        command = self.get_java_command( options, command_class )
        command += command_args
        java_env = os.environ.copy()
        if options.db_pass is not None:
            java_env[ 'EUCADW_DB_PASS' ] = options.db_pass
        popen = subprocess.Popen( command, env=java_env )
        popen.wait()
        if popen.returncode is not 0:
            sys.exit( popen.returncode )

    def command( self, parser, options, args ):
        pass

    def main_cli( self ):
        parser = OptionParser( option_list = self.options )
        parser.add_option_group( self.get_db_option_group( parser ) )
        parser.add_option_group( self.get_common_option_group( parser ) )
        (options, args) = parser.parse_args()
        options = self.add_config_defaults( options )
        try:
            self.command( parser, options, args )
        except IOError as e:
            sys.exit(e)
        except KeyboardInterrupt as k:
            sys.exit(k)






