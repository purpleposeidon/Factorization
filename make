#!/bin/bash

set -e

./recompile.sh
./reobfuscate.sh

./package


date
