#!/usr/bin/python


thread = open("thread").read()

start = "[header=2][anchor=LICENSE]License[/anchor][/header]"
end = "[/spoiler]"

i = thread.find(start) + len(start)
thread = thread[i:]
j = thread.find(end)
thread = thread[:j]
for _ in ("u", "b", "i", "pre", "list", "spoiler"):
    a = "[" + _ + "]"
    b = "[/" + _ + "]"
    thread = thread.replace(a, "").replace(b, "")

thread = thread.replace("[*]", "* ").replace("[/*]", "")
print thread
