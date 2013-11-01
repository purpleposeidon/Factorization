#!/bin/bash

set -e

echo
echo "If reinstalling, you might need to hang around to type 'Yes'..."
echo
echo

FZ=$(dirname `pwd`/$0)/

cd $FZ


pushd ../forge/ > /dev/zero
  mkdir -p accesstransformers/
  #if test ! -e common/forge_at.cfg.ORIG;
  #then
  #  cp common/forge_at.cfg common/forge_at.cfg.ORIG
  #fi
  #cat $FZ/factorization_at.cfg $FZ/chickenbones/*/*_at.cfg >> common/forge_at.cfg
  cp $FZ/factorization_at.cfg accesstransformers/
  cp $FZ/chickenbones/*/*_at.cfg accesstransformers/
  time python ./install.py
popd > /dev/zero

pushd ../forge/mcp > /dev/zero
  mkdir -p src/minecraft/factorization/
  pushd src/minecraft/factorization > /dev/zero
    ln -s $FZ/src/factorization/* ./
  popd > /dev/zero
popd > /dev/zero

./apply_ats_to_source.py

