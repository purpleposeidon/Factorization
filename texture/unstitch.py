#!/usr/bin/python


"""
If you have (linux), python, the Python Imaging Library, kolourpaint, qdbus, line.py, and a desire to unstitch your sprite sheets, then this is the script for you.

May corrupt your hard drive and eat your soul.

Usage:
  unstitch.py texture_sheet

For each non-completely-transparent 16x16 cell in the texture_sheet, it'll display it, and put out a prompt. The answers:
  exit -- exit immediately
  stop -- go to the Final Prompt
  -    -- wipe out the cell
  skip -- Leaves the cell as is, goes to the next one
  <filename> -- saves the cell to filename, making sure it ends in .png, makes sure the directory exists, wipes the cell
  +<filename> -- Same as above, but pastes it below what's already there (for animations)


Final Prompt:
  exit -- Exit without saving
  save -- Save the original texture_sheet & exit

"""


import os, os.path
from PIL import Image
import time
import sys

try:
  import readline
except ImportError:
  print "Module readline not available."
else:
  import rlcompleter
  readline.parse_and_bind("tab: complete")

def show(img, wait=""):
  img.save("/tmp/img.png")
  os.system("kolourpaint /tmp/img.png > /dev/zero 2> /dev/zero " + wait)
  time.sleep(0.5)
  action = "/kolourpaint/MainWindow_1/actions/view_fit_to_page"
  os.system('''qdbus `qdbus | grep kolourpaint | line.py "print(_.strip())"` %s trigger''' % action) 

def prompt(img):
  show(img, "&")
  action = raw_input(str(current_index) + "> ")
  os.system("killall kolourpaint")
  return action

current_index = None
current_box = None
def boxes():
  global current_index, current_box
  for y in range(0,16):
    for x in range(0,16):
      a = x*16
      b = y*16
      current_index = x, y
      current_box = (a, b, a+16, b+16)
      yield current_box

def cells():
  for box in boxes():
    c = items.crop(box)
    colorcount = c.getcolors(2)
    if colorcount != None and len(colorcount) == 1:
      if colorcount[0] == (256, (0, 0, 0, 0)):
        continue
    c.load()
    yield c

def suffix(f):
  if f.endswith(".png"): return f
  return f + ".png"

def iter_cells(img):
  for c in cells():
    cmd = prompt(c).strip()
    if not cmd: continue
    if cmd == "exit":
      raise SystemExit
    if cmd == "stop":
      return
    if cmd == "skip":
      continue
    if cmd == "-":
      remove(items)
    elif cmd[0] == "+":
      append(c, suffix(cmd[1:]) )
      remove(items)
    else:
      create(c, suffix(cmd))
      remove(items)

def remove(img):
  img.paste((64, 64, 64, 0), current_box)

def mkdirs(out):
  directory, filename = os.path.split(out)
  try:
    os.makedirs(directory)
  except OSError:
    pass
  

def append(img, out):
  if not os.path.exists(out):
    return create(img, out)
  mkdirs(out)
  orig = Image.open(out)
  b = lambda i: img.size[i] + orig.size[i]
  ret = Image.new(orig.mode, (orig.size[0], b(1)), (0, 0, 0, 0))
  ret.paste(orig, (0, 0) + orig.size)
  ret.paste(img, (0, orig.size[1] + 1))
  ret.save(out)

def create(img, out):
  mkdirs(out)
  img.save(out)

items = Image.open(sys.argv[1])
iter_cells(items)

while True:
  print "Nothing left"
  cmd = prompt(items)
  if cmd == "save":
    items.save(sys.argv[1])
    break
  elif cmd == "exit":
    break


