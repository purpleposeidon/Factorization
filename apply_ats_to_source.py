#!/usr/bin/python

import os, sys, re

PREFIX = r"^    ((:?private|protected|default|public|)( final| static)*)"


METHOD_REGEX = PREFIX + r" (\w+) (%s)(\(.*?\))$"
class method:
  def __init__(self, name):
    self.name = name
    self.src = METHOD_REGEX % self.name
    self.regex = re.compile(self.src, flags=re.MULTILINE)

  def __repr__(self): return "MD:" + self.name

  def apply(self, src):
    m = self.regex.match(src)
    if m == None: return None
    visibility, _, _, returnType, methodName, methodParameters = m.groups()
    if visibility == "public":
      print "NOTE:", methodName, "is already public"
    return "    public %s %s%s\n" % (returnType, methodName, methodParameters)

FIELD_REGEX = PREFIX + r" ((?:\w|\d|\[\])+) (%s)(.*?;)$"
class field:
  def __init__(self, name):
    self.name = name
    self.src = FIELD_REGEX % self.name
    self.regex = re.compile(self.src, flags=re.MULTILINE)
  
  def __repr__(self): return "FD:" + self.name

  def apply(self, src):
    m = self.regex.match(src)
    if m == None: return None
    evry = visibility, _, _, fieldType, fieldName, maybeInitializer = m.groups()
    if visibility == "public":
      print "NOTE:", fieldName, "is already public"
    return "    public %s %s%s\n" % (fieldType, fieldName, maybeInitializer)


fd = open("factorization_at.cfg").readlines()
tovisit = {}

def without(string, prefix):
  if not string.startswith(prefix): raise SystemExit(repr(string) + " does not start with " + prefix)
  return string[len(prefix):]

for line in fd:
  line = line.strip()
  if not line: continue
  if line.startswith('#'): continue
  modifier, notch, searge, deobf = line.split(' ')
  if searge.startswith("#FD:"):
    thingie = field
    searge = without(searge, "#FD:")
  elif searge.startswith("#MD:"):
    thingie = method
    searge = without(searge, "#MD:")
  else:
    raise SystemExit("unknown type?")
  classname = searge.split('/')[0] + '.java'
  deobf = deobf.lstrip("#")
  if not classname in tovisit: tovisit[classname] = []
  if thingie == field:
    deobf += "|" + searge.split('/')[1]
  tovisit[classname].append(thingie(deobf))


for dirname, subdirs, files in os.walk("../src/net/minecraft"):
  for f in files:
    if not f in tovisit: continue
    filename = dirname + "/" + f
    backupName = filename + ".preFzAt"
    if backupName in files:
      print filename, "was already transformed"
      continue
    ats = tovisit[f]
    src = open(filename).readlines()
    print filename
    for at in ats:
      i = 0
      found = False
      while i < len(src):
        line = src[i]
        newLine = at.apply(line)
        if newLine:
          #print `src[i]`, "-->", `newLine`
          src[i] = newLine
          found = True
          break
        i += 1
      if not found:
        print "  Failed to apply:", at.src
    open(backupName, 'w').write(open(filename).read())
    open(filename, 'w').write(''.join(src))
    
