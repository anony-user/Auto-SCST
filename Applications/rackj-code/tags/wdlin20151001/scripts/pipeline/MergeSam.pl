#!/usr/bin/perl -w

$usage = "Usage: MergeSam.pl <Main Sam File> <Append Sam File1>...<Append Sam FileN>\n";
die $usage if @ARGV == 0;

open(Sam1,$ARGV[0]);
while (<Sam1>) {
	print;
}
close(Sam1);
for $i (1 .. @ARGV-1) {
	open(Sam2,$ARGV[$i]);
	while (<Sam2>) {
		next if /^\@/;
		print;
	}
	close(Sam2);
}
