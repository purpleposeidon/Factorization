#!/bin/bash

set -e
pushd ../
  ./recompile.sh
  ./reobfuscate.sh
popd

./package


date
