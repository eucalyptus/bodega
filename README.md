Eucalyptus Data Warehouse
=========================

What is it?
-----------
The Eucalyptus Data Warehouse (or eucadw) is a reference implementation toolset for creating a data warehouse for your Eucalyptus reporting information. Reporting data that has been exported from a Cloud Controller can be imported into a database where reports can be generated.

Build and install from source
-----------------------------

In order to build the Data Warehouse from source, you will need the following:

1. An installed copy of Eucalyptus from the testing branch (the DW needs jar files located in /usr/share/eucalyptus)
2. Java 1.6.0 or greater
3. Apache Ant
4. Apache Ivy
5. Python 2.6.x

Next, check out the DW repository:

    > git clone git://github.com/eucalyptus/bodega.git
    > cd bodega

Build and install java components:

    > ant -Ddistro.name=centos
    > mkdir -p /usr/share/java/eucadw
    > cp -a lib/*.jar /usr/share/java/eucadw
    > cp -a dist/*.jar /usr/share/java/eucadw

Build and install python components:

    > cd eucadw
    > python setup.py build
    > python setup.py install


Configuring the database
------------------------

Add a repository file **/etc/yum.repos.d/eucaruntime.repo** with the following content:

    [eucaruntime]
    name = Eucalyptus Runtime
    baseurl = http://downloads.eucalyptus.com/software/eucalyptus/runtime-deps/3.1/centos/6/x86_64/
    enabled = 1
    gpgcheck = 0

Install and configure the Postgresql 9.1 packages:

    > yum install postgresql91-server
    > service postgresql-9.1 initdb
    > su - postgres
    > psql

Now, at the psql prompt run:

    > create database eucalyptus_reporting;
    > create user eucalyptus with password 'mypassword';
    > grant all on database eucalyptus_reporting to eucalyptus;
    > exit


Using the DW cli tools
----------------------

Now your system is configured to be used as a Data Warehouse. Check the status of the database by running:

    > eucadw-status -p mypassword

You can also import data exported from your Cloud Controller using:

    > eucadw-import-data -e myexportdata.txt -p mypassword

