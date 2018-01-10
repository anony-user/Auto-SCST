#!/bin/sh
#This script calls IzPack to prepare the installer
#$1 is the path to "compile"
#$2 is the kind of installers
#   standard, standard-kunststoff, web or web-kunststoff
#$3 is the name of the program

$1 install.xml -k  $2 -b .. -o $3-Installer.jar

