#!/usr/bin/python3.3


import os, json

d = "resources/assets/factorization/blockstates/"
m = "resources/assets/factorization/models/block/"

def mod(resource):
    domain, path = resource.split(':', 1)
    name = path
    if domain != 'factorization':
        print("Skipping:", resource)
        return
    path = m + path + '.json'
    if os.path.exists(path): return
    print("Creating:", path)
    json.dump({"parent": "block/cube_all", "textures": {"all": "blocks/" + name}}, open(path, 'w'), indent=4)

def recur(bull):
    for k, v in bull.items():
        if k == 'model':
            mod(v)
        else:
            recur(v)

for name in os.listdir(d):
    if not name.endswith(".json"):
        print("Skipping", name)
        continue
    bull = json.load(open(d + name))
    recur(bull)
