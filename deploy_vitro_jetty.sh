#!/bin/sh -e

set -x # Print commands and their arguments as they are executed.

#JETTY_HOME=/home/vagrant/jetty9
JETTY_HOME=/usr/local/jetty7.6.14
TARGET=./target
VITRO_HOME=./target/vdata
STAGE_DIR=/tmp/vbuild
PORT=5000

if [ $1 = "clean" ]
then
 ant clean distribute -Dskiptests=true
else
 ant distribute -Dskiptests=true
fi

mkdir -p $STAGE_DIR
mkdir -p $VITRO_HOME
tar -xvf .build/distribution.tar.gz -C $STAGE_DIR
tar -xvf $STAGE_DIR/vitrohome.tar -C $VITRO_HOME/
mkdir -p $VITRO_HOME/solr/
tar -xvf $STAGE_DIR/solrhome.tar -C $VITRO_HOME/solr/

mv $STAGE_DIR/*war $TARGET/.

cp runtime.properties $VITRO_HOME/.

mkdir -p $VITRO_HOME/config
cp applicationSetup.n3 $VITRO_HOME/config/.

#cd $JETTY_HOME
#java -jar start.jar -Djetty.port=$PORT -Dsolr.solr.home=$VITRO_HOME/solr
#-Dvitro.home=$VITRO_HOME

cd $TARGET
java -jar -Dvitro.home=./vdata -Dsolr.solr.home=./vdata/solr jetty7-runner.jar --path /vitro vitro.war --path /vitrosolr vitrosolr.war