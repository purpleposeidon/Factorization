#!/usr/bin/python


def rule(x, z):
    def base(x, z):
        a = (x/3 + z/3) % 2 == 0
        b = x % 3 == 0 and z % 3 == 0
        return a and b
    n = -1
    if base(x, z): n = 0
    if base(x + 1, z): n = 1
    if base(x, z + 1): n = 2
    if base(x + 1, z + 1): n = 3

    b = ((x+2)/6 + (z+2)/6) % 4
    if b == n: return '*'
    if n == -1: return ' '
    return ' '

out = ''
for x in xrange(-20, 25):
    for z in xrange(-80, 80):
        out += rule(x, z)
    out += '\n'

print(out)


