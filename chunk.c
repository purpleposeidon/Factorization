#include <stdio.h>
#define false 0
#define true 1

char base(int x, int z) {
    char a = (x/4 + z/4) % 3 == 0;
    char b = (x % 4 == 0) && (z % 4 == 0);
    return a && b;
}

char rule(int x, int z) {
    if (x < 0) x = -x;
    if (z < 0) z = -z;
    char n = -1;
    int N = ((x-1)/8 + (z+1)/8) % 4;
    if (base(x, z)) n = 0;
    else if (base(x + 1, z)) n = 1;
    else if (base(x, z + 1)) n = 2;
    else if (base(x + 1, z + 1)) n = 3;
    else {
        //return "012345"[N];
        return ' ';
    }
    //return "012345678"[n];
    if (N == n) return '*';
    return ' ';
}

void main() {
    for (int x = 0; x <= 45; x++) {
        for (int z = 0; z <= 80*2; z++) {
            putchar(rule(x, z));
        }
        putchar('\n');
    }
}
