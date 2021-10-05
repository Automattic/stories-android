#!/bin/bash

set -euo pipefail

# Retrieve data from previous steps
PUBLISHED_PHOTOEDITOR_VERSION=$(buildkite-agent meta-data get "PUBLISHED_PHOTOEDITOR_VERSION")

./gradlew \
    -PphotoEditorVersion="$PUBLISHED_PHOTOEDITOR_VERSION" \
    :stories:prepareToPublishToS3 $(prepare_to_publish_to_s3_params) \
    :stories:publish
