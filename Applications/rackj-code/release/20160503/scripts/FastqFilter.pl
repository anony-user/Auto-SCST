#!/usr/bin/perl 

# this program reads a fastq file, do the following filtering:
# 1. every read is trimmed to length of TrimLength
# 2. a read is filtered out if it has less than QualifiedRatio bases with base-calling
# scores less than QualityFilter

my $usage = "Usage: FastqFilter.pl <inputFastq> <trimLength> <qualityType> <qualityFilter> <qualifiedRatio> <outputFastq>\n";
my $InputFilename = $ARGV[0];
my $TrimLength = $ARGV[1];
my $QualityType = $ARGV[2];
my $QualityFilter = $ARGV[3];
my $QualifiedRatio = $ARGV[4];
my $OutputFilename = $ARGV[5];
die $usage if @ARGV != 6;

my @QualityAsciiRange;
my $QualityStart;
if ($QualityType =~ /^Sanger/i) {
	@QualityAsciiRange=(33,73);
	$QualityStart=0;
}elsif ($QualityType =~ /^Solexa/i) {
	@QualityAsciiRange=(59,104);
	$QualityStart=-5;
}elsif ($QualityType =~ /^illumina1\.0/i) {
	@QualityAsciiRange=(59,104);
	$QualityStart=-5;
}elsif ($QualityType =~ /^illumina1\.3/i) {
	@QualityAsciiRange=(64,104);
	$QualityStart=0;
}elsif ($QualityType =~ /^illumina1\.5/i) {
	@QualityAsciiRange=(66,104);
	$QualityStart=2;
}elsif ($QualityType =~ /^illumina1\.8/i) {
	@QualityAsciiRange=(33,74);
	$QualityStart=0;
}else{
	die "available qualityType: Sanger, Solexa/illumina1\.0, illumina1\.3, illumina1\.5, illumina1\.8\n";
}

my %qualityScoreMap;
my $maxScore;
my $correntScore = $QualityStart;
for my $ascii ($QualityAsciiRange[0] ... $QualityAsciiRange[1]) {
	$qualityScoreMap{chr($ascii)}=$correntScore;
	$maxScore = $correntScore;
	$correntScore++;
}
die "qualityFilter out of range\n" if ($QualityFilter<$QualityStart || $QualityFilter>$maxScore);

open(file,"<$InputFilename");
open(outfile,">$OutputFilename");
my $Passread = 0;
while($line = <file>) {
	if($. % 4 == 1) {
		$id = $line;
		$Allread++;
	}
	
	if($. % 4 == 2) {
		$seq = $line;
		$seq =~ s/\s//g;
		$seq =~ s/\./N/g;
		$seq = substr $seq, 0, $TrimLength if $TrimLength>0;
	}
	
	if($. % 4 == 3) {
		$three = $line;
	}
	
	my ($Qpass,$Qsize) = (0,0);
	if($. % 4 == 0) {
		$qal = $line;
		$qal =~ s/\s//g;
		$qal = substr $qal, 0, $TrimLength if $TrimLength>0;
		my @Qtoken = split(//,$qal);
		$Qsize = @Qtoken;
		for($j = 0; $j < $Qsize; $j++) {
			my $currScore = $qualityScoreMap{$Qtoken[$j]};
			$Qpass++ if($currScore >= $QualityFilter);
		}
		my $PassQualityRatio = ($Qpass / $Qsize);
		
		if($PassQualityRatio >= $QualifiedRatio) {
			$Passread++;
			print outfile $id;
			print outfile $seq."\n";
			print outfile $three;
			print outfile $qal."\n";
		}
	}
}
close file;
close outfile;
print "All read"."\t".$Allread."\n"."Passed read"."\t".$Passread."\n";
