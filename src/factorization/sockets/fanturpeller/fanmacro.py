#!/usr/bin/python

logic = [
    ("PumpLiquids", "!powered && !onServo && (isLiquid(source) || hasTank(source)) && (isLiquid(destination) || hasTank(destination) || isClear(destination))"),
    ("GeneratePower", "powered && (isLiquid(source) || hasTank(source)) && isClear(destination)"),
    ("BlowEntities", "isClear(front) && (isClear(back) || hasInv(back))"),
    ("MixCrafting", "hasInv(front) && hasInv(back)"),
]

templates = [
    "boolean need_X = false;",
    """if (LOGIC) {
    need_X = true;
}""",
    "if (need_X && this instanceof X) return false;",
    """if (need_X) {
    replace(coord, new X());
    return true;
}"""
]

print("// BEGIN MACRO-GENERATED CODE") # just kidding. This code isn't actually generated.
print("// The real source for this is in fanmacro.py, which should be located in the same folder as this file.")
for t in templates:
    for X, LOGIC in logic:
        print(t.replace("X", X).replace("LOGIC", LOGIC))
print("// END MACRO-GENERATED CODE")
