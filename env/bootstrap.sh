#!/usr/bin/env bash

# build eXTensible Compiler in home folder 

cd /home/vagrant
rm -rf *
apt-get update
apt-get install -y build-essential dejagnu openjdk-6-jdk
apt-get install -y unzip git-core wget
wget http://cs.nyu.edu/rgrimm/xtc/xtc-core-2.4.0.zip
wget http://cs.nyu.edu/rgrimm/xtc/xtc-testsuite-2.4.0.zip
wget http://cs.nyu.edu/rgrimm/xtc/dependencies.zip
unzip xtc-c*
mv xtc-t* dep* ./xtc && cd xtc
unzip xtc-t* && mv ./xtc/* ./  && rm -rf xtc
mv dep* ./bin && cd bin 
unzip * && rm -rf *.zip && cd .. 
rm -rf *.zip && cd .. rm -rf *.zip && cd xtc
rm -rf setup.sh && touch setup.txt
echo -e "JAVA_DEV_ROOT=/home/vagrant/xtc" >> setup.txt
echo -e "CLASSPATH=/home/vagrant/xtc/classes:/home/vagrant/xtc/bin/junit.jar:/home/vagrant/xtc/bin/javabdd.jar" >> setup.txt
echo -e "JAVA_HOME=/usr/lib/jvm/java-6-openjdk-amd64" >> setup.txt
echo -e "PATH_SEP=:" >> setup.txt
echo -e "export JAVA_DEV_ROOT CLASSPATH JAVA_HOME PATH_SEP" >> setup.txt
mv setup.txt setup.sh 
source setup.sh && make clean && make configure && make && make doc && make check
cd .. && rm -rf *.zip
echo  "FINISHED SETTING UP XTC PACKAGE"
