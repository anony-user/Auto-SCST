#!/bin/sh
#This script calls HTMLDOC to convert HTML file to PDF

HTMLDOC=/usr/local/bin/htmldoc 

$HTMLDOC --linkstyle plain --landscape --footer ../ \
         --pagemode outline --pagelayout one --size a4  --webpage \
         --firstpage toc -f UnicodeRewriterUserManual.pdf \
         UnicodeRewriterUserManual.html 

