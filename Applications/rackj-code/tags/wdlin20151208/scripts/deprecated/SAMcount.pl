#!/usr/bin/perl -w

my $SAMcount=0;
my $PREV_SAM='';
while(<>){
	next if /^\@/;
	my @t = split;
	$SAMcount++ if $t[0] ne $PREV_SAM;
	$PREV_SAM=$t[0];
}
print "SAM output read: $SAMcount\n";