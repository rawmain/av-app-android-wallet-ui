#!/bin/bash

#
# Copyright (c) 2025 European Commission
#
# Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
# Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
# except in compliance with the Licence.
#
# You may obtain a copy of the Licence at:
# https://joinup.ec.europa.eu/software/page/eupl
#
# Unless required by applicable law or agreed to in writing, software distributed under
# the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
# ANY KIND, either express or implied. See the Licence for the specific language
# governing permissions and limitations under the Licence.
#

# Fetch the latest tags from the remote
git fetch --tags

# Get the most recent tag
latest_tag=$(git describe --tags $(git rev-list --tags --max-count=1))

# Extract the base version and beta suffix
if [[ $latest_tag =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)(-beta([0-9]+))?$ ]]; then
  major=${BASH_REMATCH[1]}
  minor=${BASH_REMATCH[2]}
  patch=${BASH_REMATCH[3]}
  beta=${BASH_REMATCH[5]}

  if [[ -n $beta ]]; then
    # Increment beta version
    next_tag="v${major}.${minor}.${patch}-beta$(printf "%02d" $((beta + 1)))"
  else
    # Increment patch version
    next_tag="v${major}.${minor}.$((patch + 1))"
  fi

  echo $next_tag
else
  echo "Error: Unable to parse the latest tag."
  exit 1
fi