#!/usr/bin/perl
# Src: Velevt's shuffleSequences.pl (2-line shuffle)
# Modified for fastaq (4-line shuffle)
# Modified date: 2009/05/20
# Modifier: Yu-Jung Chang

$filenameA = $ARGV[0];
$filenameB = $ARGV[1];
$filenameOut = $ARGV[2];

open $FILEA, "< $filenameA";
open $FILEB, "< $filenameB";

open $OUTFILE, "> $filenameOut";

while(<$FILEA>) {
# print 4 lines in FILEA
	print $OUTFILE $_;
	$_ = <$FILEA>; print $OUTFILE $_; 
	$_ = <$FILEA>; print $OUTFILE $_; 
	$_ = <$FILEA>; print $OUTFILE $_; 

# print 4 lines in FILB
	$_ = <$FILEB>; print $OUTFILE $_; 
	$_ = <$FILEB>; print $OUTFILE $_;
	$_ = <$FILEB>; print $OUTFILE $_;
	$_ = <$FILEB>; print $OUTFILE $_;
}
