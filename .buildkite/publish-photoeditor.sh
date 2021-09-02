#!/bin/bash

set -euo pipefail

# Retrieve data from previous steps
PUBLISHED_MP4COMPOSE_VERSION=$(buildkite-agent meta-data get "PUBLISHED_MP4COMPOSE_VERSION")

./gradlew \
    -Pmp4composeVersion="$PUBLISHED_MP4COMPOSE_VERSION" \
    :photoeditor:prepareToPublishToS3 $(prepare_to_publish_to_s3_params) \
    :photoeditor:publish

# Add meta-data for the published version so we can use it in subsequent steps
cat ./photoeditor/build/published-version.txt | buildkite-agent meta-data set "PUBLISHED_PHOTOEDITOR_VERSION"
