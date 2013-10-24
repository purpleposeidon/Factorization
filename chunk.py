#!/usr/bin/python


#Silver ore node distribution

def silver_rule(x, z):
  return (z + 3*x) % 5 == 0

def dark_rule(x, z):
  grid = ((x//3 + z//3) % 2 == 0) 
  diag = ((x + z + 1) % 2 == 0)
  return grid and diag

rule = dark_rule


out = ''
for x in xrange(-20, 20):
  for z in xrange(-20, 20):
    out += '*' if rule(x, z) else ' '
  out += '\n'

print(out)


