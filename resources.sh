#!/bin/bash

# Convert the icon to the correct size.
convert resources/noun_87915_cc.png -resize 72x72 res/drawable-hdpi/ic_launcher.png
convert resources/noun_87915_cc.png -resize 36x36 res/drawable-ldpi/ic_launcher.png
convert resources/noun_87915_cc.png -resize 48x48 res/drawable-mdpi/ic_launcher.png
