#!/bin/bash

set -e
pushd ../forge/mcp/
  ./recompile.sh
popd

pushd ../mcp/; mkdir -p reobf/minecraft/; popd
mkdir -p ./build
OUT=./build/Factorization-dev.jar
echo A | unzip $OUT 'factorization/nei/*' -d ../mcp/reobf/minecraft/

pushd ../forge/mcp/
  ./reobfuscate_srg.sh
popd

./package


date
