#!/usr/bin/perl -w
use File::Spec;

my $usage = "Usage: sam2fas.pl <inSAM> <outFASTA>\n";
die $usage if @ARGV!=2;
my $inSAM = shift or die $usage;
my $outFAS = shift or die $usage;

open(INFILE,"<$inSAM");
open(OUTFILE,">$outFAS");
while(<INFILE>){
	next if /^\@/;
	my @t = split;
	if ($t[1]&16) {
		$t[9] =~ tr/ATCG/TAGC/;
		$t[9] = reverse $t[9];
	}
	print OUTFILE ">$t[0]\n$t[9]\n";
}
close(INFILE);
close(OUTFILE);

my ($volumeQuery,$directoriesQuery,$inName) = File::Spec->splitpath( $inSAM );
($volumeQuery,$directoriesQuery,$outName) = File::Spec->splitpath( $outFAS );

print "(SAM to FASTA) $inName -> $outName\n";