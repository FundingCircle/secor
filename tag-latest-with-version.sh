#!/bin/bash

secor_version=$(docker inspect  --format='{{.ContainerConfig.Env}}' v1.quay.io/fundingcircle/secor:latest | grep -o "SECOR_VERSION=[0-9.]\+" | cut -d'=' -f2 )
if [[ -z "${secor_version}" ]]; then
    printf "The image was built without SECOR_VERSION environment variable. Thus WON'T tag with a SECOR_VERSION number\n"
else
    printf "Tagging secor:latest with ${secor_version}\n"
    docker tag v1.quay.io/fundingcircle/secor:latest v1.quay.io/fundingcircle/secor:${secor_version}
    printf "Pushing quay.io/fundingcircle/secor:${secor_version}\n"
    docker push v1.quay.io/fundingcircle/secor:${secor_version}  
fi

