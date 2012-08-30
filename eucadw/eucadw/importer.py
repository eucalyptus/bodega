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

from optparse import OptionParser, OptionGroup
import subprocess
from eucadw import EucaDatawarehouse

class Importer(EucaDatawarehouse):

    def main_cli( self ):
        parser = OptionParser()
        parser.add_option("-f", "--file", dest="filename",
                          help="Export file for importing")
        parser.add_option("-r", "--replace", dest="replace", action="store_true",
                          help="Replace existing data")

        parser.add_option_group( self.get_db_option_group( parser ) )

        (options, args) = parser.parse_args()
        if options.filename is None:
            parser.error( 'file is required' )

        command = self.get_java_command( options, 'ImportCommand' )
        if options.replace:
            command.append( '-r' )        
        command.append( '-e' )
        command.append( options.filename )

        subprocess.call( command )
        

