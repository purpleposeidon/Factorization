This system is TeX-inspired. Just read the manual's source in this directory for examples!

Commands:
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
\obf{content}
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
\imgx{width}{height}{domain:path/to/image.png}
        Embeds an image, using the provided width and height (untested)
\img%{domain:path/to/image.png}{0.50 or something}
        Embeds an image that's been scaled down (Untested)
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
        Emits a hyperlink to the World Wide Web!
\local{localization.key}
        Translates the string given.
        (Not typeset properly? Easy to fix, honestly.)
\include{name}
        Copies in the contents of a file.

A '%' marks a line-comment.
Multiple spaces are ignored.
A single newline is ignored.
Two or more newlines create a real single newline.
Commands swallow following spaces. To actually follow a command with a space, do something like "\this\ " to expressly emit a space.



If you are in a dev environment, symlink the resources folder into MC's resource packs folder. This way you can edit the docs, save it, and press 'r' in the doc viewer to refresh.
(Also, you can press 's' to change color schemes.)

The commands /fzdoc-serialize and /html-fzdoc-export are only available in a deobf environment, or by passing -Dfz.registerDocCommands=true on the JVM command-line.

TODO: Proper macro system? :P

I've probably missed some commands.
