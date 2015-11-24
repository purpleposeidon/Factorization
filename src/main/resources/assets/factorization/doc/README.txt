This system is TeX-inspired. Just read the manual's source in this directory for examples!

If you are in a dev environment, symlink the resources folder into MC's resource packs folder.
This way you can edit the docs, save it, and press 'r' in the doc viewer to refresh.
(Resource packs need not be in zips; they can be unzipped in the resourcepacks directory to allow easier editing.)

The commands /fzdoc-serialize and /html-fzdoc-export are only available in a deobf environment, or by passing -Dfz.registerDocCommands=true on the JVM command-line.

+------------------------+
| Documentation Commands |
+------------------------+

\p      Starts a new paragraph
\nl     Emits a newline; different from \p
\-      Bulleted list item
"\ "    Emit a space; sometimes required after other commands.
\\      Emit a reverse solidus
\newpage
        Emit a new page
\leftpage
        Make the following text start at the top of a left page
\b{content}
\i{content}
\u{content}
        Apply text stylings to the content.
\title  Emit a title
\h1     Emit a header
\link{destination}{content}
        Emits a link to a different page.
\index{destination}{content}
        Like \link, but emits a following newline.
\#{item localization key}
        Embeds an item with that localization key.
        Use /f copylocalkey to get the localization key of an item.
\img{domain:path/to/image.png}
        Embeds an image. Scaled to fit. The paths used for images are ResourceLocations.
\img{domain:path/to/image.png}{0.50 or something}
        Embeds an image that's been scaled down (Untested)
\img{width}{height}{domain:path/to/image.png}
        Embeds an image, using the provided width and height (untested)
\figure{<base64-encoded compressed NBT data>}
        Embeds a figure.
        Figures are created by standing on a pillar made of gold blocks, with two gold block extensions at the base of the figure, and running the /fzdoc-serialize command.
        Due to block ID issues, only vanilla blocks and items can be safely used.
        All other items & entities will NOT have their IDs synchronized. (Except for the hard-coded FZ ones...)
        Entities should work, but are mostly untested.
        The figure is placed on a separate page, and is scaled to fit.
\generate{path}
        Embeds dynamically generated content.
        But the paths are not documented here!
        You can probably get a list of the default ones from DocumentationModule.class, but you'll still have to figure out what their format is....
\seg
\endseg
        deliminates a segment of text; the typesetter will try to prevent pagebreaks in the middle of a segment.
\topic{item localization key}
        Emits the recipes for an item.
        It is also used to index the documentation by item.
        factorization.docs.IndexDocumentation /path/to/topic_index.txt factorization=./
\checkmods{all|some|none}{space deliminated list of modIds}{enabled content}{disabled content}
        Basically an 'if' statement.
\vpad{#}
        Allows precise enlargement of the vertical padding for a line. It will have no effect if it is smaller than the line height, which is 9. Various other elements, such as items, will make the line height larger. For example, the crafting recipe list uses \vpad{15}.
\ifhtml{TRUE}{FALSE}
        Emits the TRUE branch if this is for an HTML export, or else the FALSE branch
\url{http or https address}{link text}
        Emits a hyperlink to the World Wide Web.
\local{localization.key}
        Translates the string given.
        (Not typeset properly? Easy to fix, honestly.)
\include{name}
        Copies in the contents of a file.
\for{varname}{text body, where %1 is replaced}
        Loops over the lines in varname. For each line, %1 is replaced with the text of the line.

I've probably missed some commands.

A '%' marks a line-comment.
Multiple spaces are ignored.
A single newline is ignored.
Two or more newlines create a real single newline.
Commands swallow following spaces. To actually follow a command with a space, do something like "\this\ " to expressly emit a space.

TODO: macro system

+--------------+
| IMC Commands |
+--------------+

To add an entry to Factorization's main page list:
    Send to mod: "factorization.truth"
    MessageType: "DocVar"
    format: "extraindex+={linkname}{\#{Some item} link body}"
This can be used with other \for commands, and can also be invoked with '=' instead of '+='


To register a custom recipe list:
    Send to mod: "factorization.truth"
    MessageType: "AddRecipeCategory"
    Format: String "category localization key|reference.to.classContainingRecipes|nameOfStaticFieldIterable"
    The field's value must either be Iterable or a Map, or it must be an object with a 'getRecipes' method returning Iterable or Map.

AddRecipeCategory can have pretty ugly output. To make it a bit nicer:
    Send to mod: "factorization.truth"
    MessageType: "AddRecipeCategoryGuided"
    Format: {
                "category": "Same syntax as AddRecipeCategory",
                "output": ["<ReflectionExpression>"],
                "input": ["<ReflectionExpression>"],
                "catalyst": ["<ReflectionExpression>"],
                "text": "text put underneath each recipe; perhaps used for special instructions. Uses FzDoc formatting. Optional."
            }
The components of the recipe will be printed in the order shown. "category" must be specified; the other ones are optional-ish.
A ReflectionExpression is a series of actions separated by periods, which can be one of:
    - field access: none of the below symbols
    - method invokation: ends with "()"; no parameters may be passed in, and the function may not return void
    - NBTTagCompound access: wrapped 'single quotes'.

If a null is returned anywhere, 'null' will be the result of the expression.
Fields and parameters are neither obfuscated nor deobfuscated; use only your own mod's & standard java stuff.
If you need something more complicated, register an IObjectWriter.
(Honestly, registering IObjectWriter may in fact be easier... but this doesn't give you an API dependency.)

If the ReflectionExpression ends with a '#', then everything after is used as a localization key.

Example:
        /** Convenience function for making NBT lists of strings */
        public static NBTTagList list(String ...args) {
            NBTTagList ret = new NBTTagList();
            for (String a : args) ret.appendTag(new NBTTagString(a));
            return ret;
        }

        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("category", "tile.railcraft.machine.alpha.rock.crusher.name|factorization.compat.railcraft.Compat_Railcraft|crusher_recipes");
        tag.setTag("input", list("getInput()#Input"));
        tag.setTag("output", list("getPossibleOuputs()#Output"));
        FMLInterModComms.sendMessage("factorization.truth", "AddRecipeCategoryGuided", tag);


A more complex ReflectionExpression example, this time for IC2's thermal centrifuge:
    "getValue().metadata.'minHeat'#Heat"


