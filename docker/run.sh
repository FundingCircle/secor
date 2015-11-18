#!/bin/sh

if [ -z ${SECOR_VERSION} ]; then
    printf "This script is meant to be run from within a Docker image\n \
            where the SECOR_VERSION environment variable has been set (in Dockerfile) or isn't empty.\n"
    exit 1
fi


if [ -z ${SECOR_GROUP+x} ] ||
       [ -z ${AWS_ACCESS_KEY+x} ] ||
       [ -z ${AWS_SECRET_KEY+x} ] ||
       [ -z ${ZK_QUORUM+x} ] ||
       [ -z ${SEED_BROKER_HOST+x} ] ||
       [ -z ${SEED_BROKER_PORT+x} ] ||
       [ -z ${S3_BUCKET+x} ] ||
       [ -z ${S3_PATH+x} ] ||
       [ -z ${MAX_FILE_SIZE+x} ] ||
       [ -z ${MAX_FILE_AGE+x} ]; then
    printf "A required environment variable is missing!!\n
            Can't start, won't start!!\n"
    exit 2
fi

### NOTE : Using exec because if a monitor (supervisord or envconsul) tries to kill this script
###        the signal won't be passed onto the java process(which runs in a subshell).
###        By using exec, we avoid the java process running in a subshell.
exec java -Xms512m -Xmx512m -ea -Dsecor.kafka.group=${SECOR_GROUP} \
     -Daws.access.key=${AWS_ACCESS_KEY} \
     -Daws.secret.key=${AWS_SECRET_KEY} \
     -Dzookeeper.quorum=${ZK_QUORUM} \
     -Dkafka.seed.broker.host=${SEED_BROKER_HOST} \
     -Dkafka.seed.broker.port=${SEED_BROKER_PORT} \
     -Dsecor.s3.bucket=${S3_BUCKET} \
     -Dsecor.s3.path=${S3_PATH} \
     -Dsecor.kafka.topic_filter=${TOPIC_FILTER} \
     -Dsecor.max.file.size.bytes=${MAX_FILE_SIZE} \
     -Dsecor.max.file.age.seconds=${MAX_FILE_AGE} \
     -Dlog4j.configuration=log4j.prod.properties \
     -Dconfig=secor.prod.backup.properties \
     -cp /opt/secor/secor-${SECOR_VERSION}.jar:/opt/secor/lib/* \
     com.pinterest.secor.main.ConsumerMain
