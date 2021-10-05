#!/bin/bash

set -euo pipefail

./gradlew \
    :mp4compose:prepareToPublishToS3 $(prepare_to_publish_to_s3_params) \
    :mp4compose:publish

# Add meta-data for the published version so we can use it in subsequent steps
cat ./mp4compose/build/published-version.txt | buildkite-agent meta-data set "PUBLISHED_MP4COMPOSE_VERSION"
