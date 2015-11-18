#!/bin/bash

if [ ${CIRCLE_BRANCH} == "master" ]; then
    printf "Building from the master branch means we are creating an official docker image using the docker file (Dockerfile.pinterest). \n\
This will create an image based  upon the official pinterest maven2 releases.\n\
Therefore we WON'T need to test and build the local repo\n"
else
    printf "Building from the ${CIRCLE_BRANCH} branch means we are creating an UNofficial docker image using the docker file (Dockerfile). \n\
This will create an image based upon our local repo.\n\
Therefore we will need to test and build the local repo\n"

    printf "Setting up Dependencies for the build\n"

    export PATH=$PATH:$HOME/.s3cmd
    export SECOR_LOCAL_S3=true
    export S3CMD=1.0.1

    wget https://github.com/s3tools/s3cmd/archive/v$S3CMD.tar.gz -O /tmp/s3cmd.tar.gz
    tar -xzf /tmp/s3cmd.tar.gz -C $HOME
    mv $HOME/s3cmd-$S3CMD $HOME/.s3cmd
    cd $HOME/.s3cmd && python setup.py install --user && cd -
    gem install fakes3 -v 0.1.7

    printf "\nNow testing and building\n"
    make unit && make build
fi
