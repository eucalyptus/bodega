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

from optparse import make_option as option
from eucadw import EucaDatawarehouse

class ImportData(EucaDatawarehouse):

    options = [
        option( '-f', '--file', dest="filename",
            help='Export file for importing' ),
        option( '-r', '--replace', dest='replace', action='store_true',
            help='Replace existing data'),
        ]

    def command( self, parser, options, args ):
        if options.filename is None:
            parser.error( 'file is required' )

        command = [ ]
        if options.replace:
            command.append( '-r' )        
        command.append( '-f' )
        command.append( options.filename )

        self.run_java_command( options, 'ImportCommand', command )
        

