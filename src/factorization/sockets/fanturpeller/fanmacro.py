#!/usr/bin/python

logic = [
    ("PumpLiquids", "!onServo && !powered && sourceIsLiquid && (isLiquid(destination) || hasTank(destination) || isClear(destination))"),
    ("GeneratePower", "!onServo && powered && sourceIsLiquid && isClear(destination)"),
    ("MixCrafting", "!onServo && hasInv(front) && hasInv(back)"),
    ("BlowEntities", "!sourceIsLiquid && noCollision(front)"),
]

templates = [
    "boolean need_X = false;",
    """if (LOGIC) {
    need_X = true;
}""",
    "if (need_X && this instanceof X) return false;",
    """if (need_X) {
    replaceWith(new X(), socket);
    return true;
}"""
]
START = "// BEGIN MACRO-GENERATED CODE"
END = "// END MACRO-GENERATED CODE"

contents = ""
def put(code):
    global contents
    contents += code + "\n"

put(START) # just kidding. This code isn't actually generated.
put("// The real source for this is in fanmacro.py, which should be located in the same folder as this file.")
put("// Executing it will update this code.")
for t in templates:
    for X, LOGIC in logic:
        put(t.replace("X", X).replace("LOGIC", LOGIC))
put(END)

INDENT = "\t\t"
contents = contents.strip('\n').replace("\n", "\n" + INDENT)


victim = "SocketFanturpeller.java"
f = open(victim).read()
assert START in f and END in f

front, midend = f.split(START, 1)
oldmacro, back = midend.split(END, 1)
fd = open(victim, 'w')
fd.write(front)
fd.write(contents)
fd.write(back)
