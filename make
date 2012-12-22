#!/bin/bash

set -e
pushd ../forge/mcp/
  ./recompile.sh
  ./reobfuscate.sh
popd

./package


date
