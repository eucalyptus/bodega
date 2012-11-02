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

import os
from optparse import make_option as option
from eucadw import EucaDatawarehouse

class GenerateReport(EucaDatawarehouse):

    options = [
        option( '-t', '--type',
            dest='type', type='choice',
            choices=['elastic-ip', 'instance', 's3', 'snapshot', 'volume'],
            help='the report type to generate'),
        option( '-f', '--format',
            dest='format', type='choice',
            choices=['csv', 'html'],
            help='the format for the generated report'),
        option( '-s', '--start-date', dest='start_date',
            help='the inclusive start date for the report period (e.g. 2012-08-19)'),
        option( '-e', '--end-date', dest='end_date',
            help='the exclusive end date for the report period (e.g. 2012-08-26)'),
        option( '--time-unit', dest='time_unit', type='choice',
            choices=['seconds', 'minutes', 'hours', 'days'],
            help='the time unit to use in reports'),
        option( '--size-unit', dest='size_unit', type='choice',
            choices=['b', 'kb', 'mb', 'gb'],
            help='the size unit to use in reports'),
        option( '--size-time-time-unit', dest='size_time_time_unit', type='choice',
            choices=['seconds', 'minutes', 'hours', 'days'],
            help='the time unit to use in reports'),
        option( '--size-time-size-unit', dest='size_time_size_unit', type='choice',
            choices=['b', 'kb', 'mb', 'gb'],
            help='the size unit to use in reports'),
        option( '-F', '--force', dest='force', const=True, action='store_const',
            help='overwrite output file if it exists' ),
        ]

    def check_report_file(self, file):
        if os.path.exists( file ):
            msg = 'file %s already exists, ' % file
            msg += 'please remove and try again'
            raise IOError(msg)

    def timestamp( self, date ):
        return date + 'T00:00:00'

    def command( self, parser, options, args ):
        self.force = options.force
        filename = args[0] if 0 < len(args) else None
        if not self.force and filename is not None:
            self.check_report_file( filename )

        command = [ ]
        if options.type is not None:
            command.append( '-t' )
            command.append( options.type )
        if options.format is not None:
            command.append( '-f' )
            command.append( options.format )
        if options.start_date is not None:
            command.append( '-s' )
            command.append( self.timestamp( options.start_date ) )
        if options.end_date is not None:
            command.append( '-e' )
            command.append( self.timestamp( options.end_date ) )
        if options.time_unit is not None:
            command.append( '-tu' )
            command.append( options.time_unit )
        if options.size_unit is not None:
            command.append( '-su' )
            command.append( options.size_unit )
        if options.size_time_time_unit is not None:
            command.append( '-sttu' )
            command.append( options.size_time_time_unit )
        if options.size_time_size_unit is not None:
            command.append( '-stsu' )
            command.append( options.size_time_size_unit )
        if filename is not None:
            command.append( '-r' )
            command.append( filename )

        self.run_java_command( options, 'ReportCommand', command )


