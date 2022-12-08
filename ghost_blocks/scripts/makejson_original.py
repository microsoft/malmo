"""Somewhat simple Minecraft model JSON generator written in Python.

Usage: python makejson.py [block|item] filename [subtypeCount] [otherOptions]

Other options (type "option=value"):
layer -- The layer the texture will be on (lower layers will get a texture called "blank")
texture -- The name of the texture to use. Default is the same as the file name.
type -- The parent model for items. Default is "generated" and should not be changed in most cases.

Examples:
python makejson.py item gem 16
python makejson.py item myitem texture=mytexture
python makejson.py block the_block texture=derp_face
"""

import sys
import re
import os

# Your mod ID. Should be all lowercase letters and underscores, nothing else. This absolutely must match your mod ID!
MOD_ID = 'tutorial'
# The directories to write the files to. I let the script write to an output directory, then copy
# the files to my resources folder when I'm happy with the results. You could change this to output
# directly to your resources folder, but I would not recommend it.
DIR_OUTPUT_BLOCKSTATES = 'output/blockstates/'
DIR_OUTPUT_BLOCKS = 'output/models/block/'
DIR_OUTPUT_ITEMS = 'output/models/item/'

def createDirIfNeeded(name):
    """Create a directory if it does not exist."""
    if not os.path.exists(name):
        os.makedirs(name)

def createAllDirs():
    """Create all directories we may need."""
    createDirIfNeeded(DIR_OUTPUT_BLOCKSTATES)
    createDirIfNeeded(DIR_OUTPUT_BLOCKS)
    createDirIfNeeded(DIR_OUTPUT_ITEMS)

def writeBlockJSONs(name, texture):
    """Creates the three JSON files needed for a block.

    Arguments:
    name -- The name to give the files (.json is automatically appended)
    texture -- The name of the texture to use.
    """
    print('Writing block %s (texture %s)' % (name, texture))

    #blockstate
    f = open(DIR_OUTPUT_BLOCKSTATES + name + '.json', 'w')
    f.write('{\n')
    f.write('  "variants": {\n')
    f.write('    "normal": { "model": "%s:%s" }\n' % (MOD_ID, name))
    f.write('  }\n')
    f.write('}\n')
    f.close()

    #block model
    f = open(DIR_OUTPUT_BLOCKS + name + '.json', 'w')
    f.write('{\n')
    f.write('  "parent": "block/cube_all",\n')
    f.write('  "textures": {\n')
    f.write('    "all": "%s:blocks/%s"\n' % (MOD_ID, texture))
    f.write('  }\n')
    f.write('}\n')
    f.close()

    #item model
    f = open(DIR_OUTPUT_ITEMS + name + '.json', 'w')
    f.write('{\n')
    f.write('  "parent": "%s:block/%s",\n' % (MOD_ID, name))
    f.write('  "textures": {\n')
    f.write('    "layer0": "%s:blocks/%s"\n' % (MOD_ID, texture))
    f.write('  }\n')
    f.write('}\n')
    f.close()

def writeItemJSON(name, texture, layer=0, item_type='generated'):
    """Creates the JSON file needed for an item. Multi-layer models can be
    created, but the textures for all but the highest layer cannot be named. In
    those cases, you will need to either modify the files yourself, or modify
    this script to fit your needs.

    Arguments:
    name -- The name to give the file (.json is automatically appended)
    texture -- The name of the texture to use. Only the highest layer can be named.
    layer -- The index of the highest layer (layers start at 0)
    item_type -- The 'parent' model for the item.
    """
    print('Writing item %s (texture %s)' % (name, texture))

    f = open(DIR_OUTPUT_ITEMS + name + '.json', 'w')
    f.write('{\n')
    f.write('  "parent": "item/%s",\n' % item_type)
    f.write('  "textures": {\n')
    for i in range(0, layer):
        f.write('    "layer%d": "%s:items/Blank",\n' % (i, MOD_ID))
    f.write('    "layer%d": "%s:items/%s"\n' % (layer, MOD_ID, texture))
    f.write('  }\n')
    f.write('}\n')
    f.write('\n')
    f.close()



# regex to match numbers
numRegex = re.compile("^\d+$")

# Blocks require 3 files to be created, items only need 1.
isBlock = False
# Name for the file(s) being created
name = ''
# The texture name. Typically the same as name, but a different value can be specified.
texture = ''
# The number of subtypes. If greater than 1, it will create files like name0, name1, etc.
count = 1
# The layer the texture should be on. If greater than 0, lower layers will be given a texture called "blank".
# If you need to use this for any reason (unlikely) create a fully transparent item texture and name it "blank".
# This is only for items.
layer = 0
# The "parent" for items. Typically you want "generated".
type = 'generated'



# read command line arguments
for arg in sys.argv:
    # lowercase argument for matching purposes
    argl = str.lower(arg)
    matchNum = numRegex.match(arg)
    # try to match some optional arguments
    matchCount = re.compile('count=').match(argl)
    matchLayer = re.compile('layer=').match(argl)
    matchTexture = re.compile('texture=').match(argl)
    matchType = re.compile('type=').match(argl)

    # Block or item? Default is item.
    if argl == 'block':
        isBlock = True
    elif argl == 'item':
        isBlock = False

    if matchNum or matchCount: # subtype count
        count = int(matchNum.group(0))
    elif matchLayer: # layer index
        layer = int(re.search('\d+', argl).group(0))
    elif matchTexture: # texture name
        texture = re.sub('texture=', '', arg)
    elif matchType: # item parent model
        type = re.sub('type=', '', arg)
    elif arg != 'makejson.py':
        name = arg

if name == '': # name must be provided in command line!
    print('No block/item name specified!')
    exit(1)
if texture == '': # if no texture specified, use file name
    texture = name
if count < 1: # just in case user does something silly...
    count = 1

# create the output directories...
createAllDirs()

for i in range(count):
    filename = name
    textureName = texture
    # if we have subtypes, append the index
    if count > 1:
        filename += str(i)
        textureName += str(i)
    # write the file(s)!
    if isBlock:
        writeBlockJSONs(filename, textureName)
    else:
        writeItemJSON(filename, textureName, layer, type)
