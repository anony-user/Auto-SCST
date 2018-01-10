#!/usr/bin/perl -w
use IPC::Open2;

my $usage = "Usage: MastCompare.pl <targetMast> <backgroundMast> <numberTarget> <numberBackground> <backgroundContainingTarget(0/1)> <rackjJARpath>\n";
die $usage if @ARGV<6;

my $targetMast = shift or die $usage;
my $backgroundMast = shift or die $usage;
my $targetNum = shift or die $usage;
my $backgroundNum = shift or die $usage;
my $containingTarget = shift;
my $rackjJarPath = shift or die $usage;

my %targetMap;
my %backgroundMap;

$debug=0;

print "parameters: $targetMast $backgroundMast $targetNum $backgroundNum $containingTarget $rackjJarPath\n" if $debug;

# Collect [motif],[gene],[p-value]
open(TARGET,"<$targetMast");
while (<TARGET>) {
	print "target in: $_" if $debug;
	next if $_=~/^#/;
	my @token=split(/\s+/,$_);
	$token[1] =~ /[+|-]?(\d+)/;
	my $motif=$1;
	my $length=$token[3]-$token[2]+1;
	$targetMap{$motif}{$token[0]}{$token[5]}=$length;
	print "target: $motif $token[0] $token[5] $length\n" if $debug;
}
open(BACKGROUND,"<$backgroundMast");
while (<BACKGROUND>) {
	print "background in: $_" if $debug;
	next if $_=~/^#/;
	my @token=split(/\s+/,$_);
	$token[1] =~ /[+|-]?(\d+)/;
	my $motif=$1;
	my $length=$token[3]-$token[2]+1;
	$backgroundMap{$motif}{$token[0]}{$token[5]}=$length;
	print "background: $motif $token[0] $token[5] $length\n" if $debug;
}

my $pid = open2( *fisherOUT, *fisherIN,"java -classpath $rackjJarPath statistics.FisherExactTest");

print "Motif\tLength\tThreshold\tfisher P-value\tA\tB\tC\tD\n";
for my $motif (sort {$a <=> $b} (keys %targetMap) ) {
	my $minPV=1;
	my %targetList;
	my %wholeList;
	my @motifLength;
	
	# retrieve target list [gene]-[min p-value] set
	for my $gene (sort {$a cmp $b} keys %{$targetMap{$motif}} ) {
		for my $pv (sort {$a <=> $b} keys %{$targetMap{$motif}{$gene}} ) {
			$minPV=$pv if $pv < $minPV;
			push(@{$targetList{$gene}},$pv);  #TargetGene set
			$motifLength[$motif]=$targetMap{$motif}{$gene}{$pv};
			last;
		}
	}
	
	# retrieve whole genome [gene]-[min p-value] set
	for my $gene (keys %{$backgroundMap{$motif}} ) {
		for my $pv (sort {$a <=> $b} keys %{$backgroundMap{$motif}{$gene}} ) {
			push(@{$wholeList{$gene}},$pv);  #WholeGene set
			last;
		}
	}
	
	# Search Threshold values
	my $minTH = 0.1;
	my @Thresholds;
	while ( $minTH > $minPV) { $minTH=$minTH/10; }
	$minTH=$minTH*5 if $minTH < $minPV;	
	$minTH=$minTH*2 if $minTH < $minPV;

	if ($minTH =~/^5e|0\.0*5$/i) {
		push (@Thresholds,$minTH);
		$minTH=$minTH*2;
	}
	
	while ($minTH <=1.00001) {
		push (@Thresholds,$minTH);
		$minTH=$minTH*5;
		push (@Thresholds,$minTH) if $minTH <=1;
		$minTH=$minTH*2;
	}
	
	for my $Threshold (@Thresholds) {
		my @oTarget;
		my @xTarget;
		my @oBackground;
		#my @Whole;
		
		for my $gene (keys %targetList) {
			my $pv = $targetList{$gene}[0];
			if ($pv<=$Threshold) {
				push(@oTarget,$gene);   #Target(o)
			}
		}
		
		for my $gene (keys %wholeList) {
			my $pv = $wholeList{$gene}[0];
			if ($pv<=$Threshold) {
				push(@oBackground,$gene);   #Whole(o)
			}
		}
		
		my $A = @oTarget;

		my $B;    #oBackground
		if($containingTarget){
			$B = @oBackground - @oTarget;
		}else{
			$B = @oBackground;
		}

		my $C = $targetNum - @oTarget;      #xTarget

		my $D;   #xBackground;
		if($containingTarget){
			$D = $backgroundNum - $A - $B - $C;
		}else{
			$D = $backgroundNum - $B;
		}

		print fisherIN "$A $B $C $D\n";
		my $msg = <fisherOUT>;
		my @fTokens=split(/\s+/,$msg);
		print "$motif\t$motifLength[$motif]\t$Threshold\t$fTokens[2]\t$A\t$B\t$C\t$D\n";
	}
}
close(fisherOUT);
close(fisherIN);
