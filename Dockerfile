<<<<<<< ff74c8c9a244855c842e1756e0df216d4b8c1393
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
=======
FROM java:8

RUN mkdir -p /opt/secor
ADD target/secor-*-bin.tar.gz /opt/secor/

COPY src/main/scripts/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
>>>>>>> Docker image for secor
