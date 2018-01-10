#!/usr/bin/perl -w

my $usage = "Usage: intronExonAvgDepth.pl <CGFF> <geneCoverage>\n";

my $cgff = shift or die $usage;
my $geneCoverage = shift or die $usage;

my $bName = $geneCoverage;
$bName =~ s/\.[^.]+$//;


my %cgffRegionHoA;
open(CGFF,"<$cgff");
my $gene;
my $gStart;
while(<CGFF>) {
	s/\s+$//;
	my @token = split;
	if(/^>/){
		$gene = substr $token[0], 1;
		$gStart = $token[2];
	}else{
		push @{$cgffRegionHoA{$gene}}, ($token[0]-$gStart, $token[1]-$gStart); 
	}
}
close(CGFF);


my %geneExonCovAvgHoH;
my %geneIntronCovAvgHoH;
open(COVRG,"<$geneCoverage");
while(<COVRG>){
	s/\s+$//;
	next if /^#/;
    my @token = split(/\t/);
    my $geneID = $token[0];
    $token[4]=~/\[(.+)\]/;
    my @coverageArray = split(/\,\s/, $1);
    
    # exon
    my $exonNum = 1;
    for(my $i=0; $i<@{$cgffRegionHoA{$geneID}}; $i+=2){
    	my $start = ${$cgffRegionHoA{$geneID}}[$i];
    	my $stop  = ${$cgffRegionHoA{$geneID}}[$i+1];
    	my $len = $stop-$start+1;
    	my $rate = 0;
    	
    	my @exonCoverage = @coverageArray[$start..$stop];
    	my $sumExonCov=0;
    	$sumExonCov += $_ for @exonCoverage;

    	for my $value (@exonCoverage){
    	    $rate+=1 if $value>0;
    	}
    	$rate /= $len;
    	
    	my $avgExonCov = $sumExonCov / $len;
    	push @{$geneExonCovAvgHoH{$geneID}{$exonNum}}, ($avgExonCov, $len, $rate);
    	$exonNum++;
    }
    
    # intron  
    my $intronNum = 1;
    for(my $j=1; $j<@{$cgffRegionHoA{$geneID}}-1; $j+=2){
    	my $start = ${$cgffRegionHoA{$geneID}}[$j]+1;
    	my $stop = ${$cgffRegionHoA{$geneID}}[$j+1]-1;
    	my $len = $stop-$start+1;
    	my $rate = 0;
    	
    	my @intronCoverage = @coverageArray[$start..$stop];
    	my $sumIntronCov=0;
    	$sumIntronCov += $_ for @intronCoverage;
    	
    	for my $value (@intronCoverage){
    	    $rate+=1 if $value>0;
    	}
    	$rate /= $len;
    	
    	my $avgIntronCov = $sumIntronCov / $len;
    	push @{$geneIntronCovAvgHoH{$geneID}{$intronNum}}, ($avgIntronCov, $len, $rate);
    	$intronNum++;
    }
}
close(COVRG);


# output
my $intronCountFile = $bName.".depth.intronCount";
open(iOUT,">$intronCountFile");
print iOUT "#GeneID\tintronNo\tdepth\tintronLen\tmulti/ALL\tcoveredRatio\tformat:.intronCount\n";
for my $key(sort keys %geneIntronCovAvgHoH){
	for my $num(sort {$a<=>$b} keys %{$geneIntronCovAvgHoH{$key}}){
		print iOUT "$key\t$num\t".join("\t",@{$geneIntronCovAvgHoH{$key}{$num}}[0..1])."\t0\t$geneIntronCovAvgHoH{$key}{$num}[2]\n";
	}
}
close(iOUT);


my $exonCountFile = $bName.".depth.exonCount";
open(eOUT,">$exonCountFile");
print eOUT "#GeneID\texonNo\tdepth\texonLen\tmulti/ALL\tcoveredRatio\tformat:.exonCount\n";
for my $key(sort keys %geneExonCovAvgHoH){
	for my $num(sort {$a<=>$b} keys %{$geneExonCovAvgHoH{$key}}){
		print eOUT "$key\t$num\t".join("\t",@{$geneExonCovAvgHoH{$key}{$num}}[0..1])."\t0\t$geneExonCovAvgHoH{$key}{$num}[2]\n";
	}
}
close(eOUT);


