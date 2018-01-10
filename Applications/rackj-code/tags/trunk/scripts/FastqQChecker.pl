#!/usr/bin/perl 

my $usage = "Usage: FastqQChecker.pl inputFastq qualityType\n";
my $inFilename = shift or die $usage;
my $QualityType = shift or die $usage;

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

open(file,"<$inFilename");
my @baseQualHash = ();
while($qal = <file>) {
	if($. % 4 == 0) {
		$qal =~ s/\s//g;
		my @Qtoken = split(//,$qal);
		$Qsize = @Qtoken;
		for($j = 0; $j < $Qsize; $j++) {
			my $currScore = ord($Qtoken[$j])-$QualityAsciiRange[0]+$QualityStart;
			#my $currScore = $qualityScoreMap{$Qtoken[$j]};
			$baseQualHash[$j]{$currScore}++;
		}
	}
}
close file;

for(my $baseIdx=0; $baseIdx<@baseQualHash; $baseIdx++){
	my $qSum = 0;
	my $numSeq = 0;
	for $score (keys %{$baseQualHash[$baseIdx]}){
		$qSum += ($score*$baseQualHash[$baseIdx]{$score});
		$numSeq += $baseQualHash[$baseIdx]{$score};
	}
	my $avg = $qSum/$numSeq;
	print "$baseIdx\t$avg\n";
}
