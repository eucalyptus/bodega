#!/usr/bin/env python

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

import sys
import os
from distutils.command.build_scripts import build_scripts
from distutils.core import setup
from distutils.sysconfig import get_python_lib
import fileinput

prefix  = '/opt/datawarehouse'
version = '3.2'

class build_scripts_with_path_headers(build_scripts):
    def run(self):
        build_scripts.run(self)
        self.path_header = get_python_lib(prefix=prefix).replace('dist-packages', 'site-packages')
        self.outfiles = [os.path.join(self.build_dir, os.path.basename(script))
                         for script in self.distribution.scripts]
        self.add_paths_to_scripts()

    def add_paths_to_scripts(self):
        print 'adding path %s to scripts' % self.path_header
        for line in fileinput.input(self.outfiles, inplace=1, backup=None):
            if fileinput.isfirstline():
                print line.rstrip()
                print 'import sys'
                print 'sys.path.append("%s")' % self.path_header
            elif line.strip() == 'import sys':
                pass
            elif line.strip().startswith('sys.path.append'):
                pass
            else:
                print line.rstrip()

admin_scripts = ["bin/eucadw-import",                 
                ]

setup(name="eucadw",
      version=version,
      description="Eucalyptus Datawarehouse Tools",
      long_description="CLI tools for use with reporting data warehouse",
      url="http://eucalyptus.com/",
      packages=['eucadw'],
      license='GPLv3',
      platforms='Posix; MacOS X; Windows',
      classifiers=[ 'Development Status :: 5 - Production/Stable',
                      'Operating System :: OS Independent',
                      'Topic :: Internet',
                      ],
      cmdclass={'build_scripts': build_scripts_with_path_headers},
      scripts=admin_scripts,
      )

