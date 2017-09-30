#!/bin/bash

pattern="tachiyomi-r*"
files=( $pattern )
export ARTIFACT="${files[0]}"

if [ -z "$ARTIFACT" ]; then
    echo "Artifact not found"
    exit 1
fi

export SSHOPTIONS="-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i ${DEPLOY_KEY}"

scp $SSHOPTIONS $ARTIFACT $DEPLOY_USER@$DEPLOY_HOST:builds/
ssh $SSHOPTIONS $DEPLOY_USER@$DEPLOY_HOST ln -sf builds/$ARTIFACT builds/latest
