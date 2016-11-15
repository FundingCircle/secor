FROM quay.io/fundingcircle/alpine-java:latest

MAINTAINER fundingcircle "engineering@fundingcircle.com"

# This environment variable is used here and in the /opt/secor/run.sh script
# It should be updated to build images for new versions
ENV SECOR_VERSION 0.23-SNAPSHOT

COPY ./target/secor-$SECOR_VERSION-bin.tar.gz  /tmp/
RUN mkdir -p /opt/secor \
        && tar -zxvf /tmp/secor-$SECOR_VERSION-bin.tar.gz -C /opt/secor \
        && rm -rf /tmp/secor-$SECOR_VERSION-bin.tar.gz
COPY ./docker/run.sh /opt/secor/run.sh

WORKDIR /opt/secor

ENTRYPOINT ["/usr/bin/envconsul -prefix secor/config /opt/secor/run.sh"]
