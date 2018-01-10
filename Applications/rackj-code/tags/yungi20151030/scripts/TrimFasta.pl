#!/usr/bin/perl

$filenameIn = $ARGV[0];
$TrimLength = $ARGV[1];
$filenameOut = $ARGV[2];

open $FILEIN, "< $filenameIn";

open $OUTFILE, "> $filenameOut";

while(<$FILEIN>) {
# print 2 lines in FILEIN
	print $OUTFILE trim($_)."\n";
	$_ = <$FILEIN>; print $OUTFILE trim(substr $_, 0, $TrimLength)."\n";
}

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}
