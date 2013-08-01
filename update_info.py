#!/usr/bin/python

import json

info = json.load(open("mcmod.info"))
versioning_file = "src/factorization/common/Core.java"

version = None
for line in open(versioning_file):
  if "@VERSION@" in line:
    version = line.split("=")[1].split(";")[0].strip().strip('"')
    break


if __name__ == '__main__':
  print "Version:", version
info[0]["version"] = version
json.dump(info, open("mcmod.info", 'w'), indent=1, sort_keys=True)

