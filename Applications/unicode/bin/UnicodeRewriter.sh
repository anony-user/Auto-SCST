#!/bin/sh
# This script launches Unicode Rewriter

# Java 1.4.2 or above is needed
##################################################################
## Modify this so that the correct Java virtual machine is used ##
JAVA=/usr/bin/java
##################################################################

$JAVA -jar UnicodeRewriter.jar $*
