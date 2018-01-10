#!/usr/bin/perl -w

use strict;
use File::Basename;
use Getopt::Long;
use List::Util qw(sum);

# All geneRPKM files contain same number of genes listing in the same order
# The default is the correlation of RPKM if -read or -uniq option is not specified
# When -log option is specified, log transformation is taken before computing 


my $usage="usage: RPKMcorrelation.pl [ -read | -uniq ] [ -log ] [ -multiratio_cutoff <value> ] [ -maxCutoff <value> ] [ -o <output file> ] <geneRPKM 1, geneRPKM 2, ...>\n";

my ($reads,$uniq,$lg,$multiRatioCutOff,$maxCutoff,$output)=(0,0,0,0.96,-1,"");
GetOptions ('read' => \$reads, 'uniq' => \$uniq, 'log' => \$lg, 'multiratio_cutoff=f' => \$multiRatioCutOff, 'maxCutoff=f' => \$maxCutoff, 'o=s' => \$output) or die $usage;

die $usage if @ARGV<2;

my @inFiles;
while(@ARGV){
	push @inFiles, shift;
}


my %valHoH;
my %geneIds;
my %removeId;
my @fileOrder;
my %sortedValHoA;

# read files
for my $infile (@inFiles){
	my $filename=fileparse($infile, ".geneRPKM");
	push @fileOrder, $filename;
	
	open(INFILE,"<$infile");
	while(<INFILE>){
		s/\s+$//;
		next if($_ =~ /^#/);

		my @arr = split(/\s/, $_);
		my $geneId = $arr[0];
		my $readCnt = $arr[2];
		my $RPKM = $arr[3];
		my $multiRatio = $arr[4];
		my $uniqRead = $readCnt*(1-$multiRatio);
		my $value = $RPKM;
		if($reads){
			$value = $readCnt;
		}elsif($uniq){
			$value = $uniqRead;
		}
		$geneIds{$geneId}=1;
		$valHoH{$filename}{$geneId}=$value;
		
		# to be removed if multi ratio high in some sample
		$removeId{$geneId}=1 if($multiRatio>$multiRatioCutOff);
	}
}

# to be removed if RPKM all lower than threshold
for my $geneId (keys %geneIds) {
	my $max = 0;
	for my $filename (keys %valHoH) {
		$max = $valHoH{$filename}{$geneId} if $valHoH{$filename}{$geneId}>$max;
	}
	$removeId{$geneId} = 1 if $max<$maxCutoff;
}

# remove records
for my $id (keys %removeId){
	for my $fn (keys %valHoH){
		delete $valHoH{$fn}{$id};
	}
}

if($lg){
	my $minNonZero;
	for my $filename (keys %valHoH) {
		for my $geneId (keys $valHoH{$filename}) {
			if($valHoH{$filename}{$geneId}>0 && (!defined $minNonZero || $valHoH{$filename}{$geneId}<$minNonZero)){
				$minNonZero=$valHoH{$filename}{$geneId};
			}
		}
	}

	for my $filename (keys %valHoH) {
		for my $geneId (keys $valHoH{$filename}) {
			if($valHoH{$filename}{$geneId}==0){
				$valHoH{$filename}{$geneId} = log(($minNonZero/2))/log(2);
			}else{
				$valHoH{$filename}{$geneId} = log($valHoH{$filename}{$geneId})/log(2);
			}
		}	
	}
}

for my $filename (sort (keys %valHoH)){
	for my $geneId (sort (keys $valHoH{$filename})){
		push @{$sortedValHoA{$filename}}, $valHoH{$filename}{$geneId};
	}
}


# output
my $fHandler;
if($output ne ""){
	open($fHandler,">",$output);
}else{
	$fHandler = \*STDOUT;
}

my $str = "";
print $fHandler "\t".join("\t", @fileOrder[0..($#fileOrder-1)])."\n";

for(my $i=1; $i<=$#fileOrder; $i++) {
	my $fn1=$fileOrder[$i];
	$str = $fn1;

	for(my $j=0; $j<$i; $j++){
		my $fn2=$fileOrder[$j];
		
		my $r = correlation( \@{$sortedValHoA{$fn1}}, \@{$sortedValHoA{$fn2}} );
		$str .= "\t$r";
	}
	print $fHandler "$str\n";
}

close($fHandler);


#-------------------------------
# subroution
#-------------------------------
sub correlation {
	my @xArr=@{$_[0]};
	my @yArr=@{$_[1]};
	die("empty array") unless (@xArr && @yArr);
	die("array must be the same length") unless (@xArr==@yArr);
	
	my $len=@xArr;
	
	my $xMean = mean(\@xArr);
	my $yMean = mean(\@yArr);
	
	my $xStd = stdev(\@xArr);
	my $yStd = stdev(\@yArr);
	
	my $accum=0;
	for my $i (0..($len-1)){
		$accum += (($xArr[$i]-$xMean)*($yArr[$i]-$yMean));
	}
	
	my $r = $accum / (($len-1)*$xStd*$yStd);
	return $r;
}


sub mean {
	my @arr = @{$_[0]};
	return sum(@arr)/@arr;	
}

sub stdev {
	my @arr = @{$_[0]};
	if(@arr==1){
		return 0;
	}
	
	my $mean = mean(\@arr);
	
	my $accum = 0;
	foreach(@arr){
		$accum += ($_-$mean)**2;
	}
	
	my $variance = $accum/(@arr-1);
	
	return sqrt($variance);
}


