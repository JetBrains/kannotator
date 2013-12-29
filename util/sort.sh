#!/bin/sh

rm -rf compare/jdk-annotations-snapshot
rm -rf compare/jdk-annotations
find jdk-annotations -type f -exec xsltproc --output compare/"{}" util/sort.xml "{}" \;
find jdk-annotations-snapshot -type f -exec xsltproc --output compare/"{}" util/sort.xml "{}" \;
rm -rf compare/jdk-annotations-snapshot/com
rm -rf compare/jdk-annotations-snapshot/sun
