#!/usr/bin/python


#Silver ore node distribution

def rule(x, z):
  return (z + 3*x) % 5 == 0


out = ''
for x in xrange(-20, 20):
  for z in xrange(-20, 20):
    out += '*' if rule(x, z) else ' '
  out += '\n'

print(out)


