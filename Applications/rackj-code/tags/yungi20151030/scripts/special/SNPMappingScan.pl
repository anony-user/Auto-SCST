#!/usr/bin/perl -w
use List::Util 'max';

my $usage = "USAGE: SNPmappingScan.pl <Nucleotide Freq File> <window size> <step size> <heter threshold> [<chromosome> <start> <stop>]\n";

my $ntFreqFile = shift or die $usage;
my $window_size = shift or die $usage; # 5000;
my $step_size = shift or die $usage; # 100;
my $heterThd = shift or die $usage; # 0.1

my $zeroReplacement = 1;

my $searchChr;
my $searchStart;
my $searchStop;

#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-zero') {
		$zeroReplacement=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

# rest parameters
if(@ARGV>0){
	if(@ARGV!=3){
		die $usage;
	}else{
		$searchChr = shift;
		$searchStart = shift;
		$searchStop = shift;
	}
}

my %CodeMap = ('A' => 3,'T' => 4,'G' => 5,'C' => 6,'N' => 7);
my %ChrSNPrec;

open(FREQIN, "<$ntFreqFile");
while(<FREQIN>) {
	next if $. == 1;
	my @t = split(/\s/);
	my @subToken = split(/[:|\-|>]/,$t[0]);
	my $chr=$subToken[0];
	my $position=$subToken[1];
	next if $subToken[2] !~ /[A|T|C|G|N]/;
	
	my $aIDX=$CodeMap{$subToken[2]};
	my $bIDX=$CodeMap{$subToken[4]};
	my $numA=$t[$aIDX];
	my $numB=$t[$bIDX];
	
	$ChrSNPrec{$chr}{$position}=[$numA,$numB];
}
close FREQIN;

print "#Chr\tWindow start\ttypeA\tBoth\ttypeB\tHomo/Hetero\tratio(typeA)\tratio(typeB)\n"; # report Header

for $chr (sort (keys (%ChrSNPrec))) {
	
	if(defined $searchChr){
		next if $searchChr ne $chr;
	}
	
	my $maxLength = max keys %{$ChrSNPrec{$chr}};
	if(defined $searchStop){
		$maxLength = $searchStop + $window_size;
	}
	
	my @positionArray = sort {$a <=> $b} keys %{$ChrSNPrec{$chr}};
	my $currPosition=1;
	if(defined $searchStart){
		while($positionArray[0]<$searchStart){
			shift @positionArray;
		}
		$currPosition = $searchStart;
	}

	my $windowTail=$currPosition+$window_size-1;
	
	my @windowArray;
	while ($windowTail<=$maxLength) {
		# Adjust window_Array head
		while (@windowArray>0 && $windowArray[0] < $currPosition) { shift(@windowArray); }
		# Adjust window_Array tail
		while (@positionArray>0 && $positionArray[0]<$windowTail && (@windowArray == 0 || $windowArray[-1]<$windowTail)) {
			push(@windowArray,shift(@positionArray));
		}
		
		my $typeAReadNum=0;
		my $ReadSum=0;
		
		#print "Range=$chr \[$currPosition...$windowTail\]\n" if @windowArray>0;
		#print join(" ",@windowArray)."\n" if @windowArray>0 ;
		my $typeACnt=0;
		my $bothCount=0;
		my $typeBCnt=0;
		
		if (@windowArray>0) {
			for my $position (@windowArray) {
				$typeAReadNum = $ChrSNPrec{$chr}{$position}[0];
				$ReadSum = $ChrSNPrec{$chr}{$position}[0]+$ChrSNPrec{$chr}{$position}[1];
				
				#print "$chr : $currPosition -> $typeAReadNum\t$ReadSum\n";
				if ($ReadSum>0) {
					my $SNPratio = $typeAReadNum/$ReadSum;
					
					if ($SNPratio<=$heterThd) {
						$typeBCnt++;
					}elsif ($SNPratio >$heterThd && $SNPratio <(1-$heterThd)) {
						$bothCount++;
					}elsif ($SNPratio >=(1-$heterThd)) {
						$typeACnt++;
					}
				}
			}
		}
		my $homoHeter;
		eval{ $homoHeter=($typeACnt+$typeBCnt)/$bothCount };
		$homoHeter = ($typeACnt+$typeBCnt) / $zeroReplacement if $@;
		
		my $typeAratio;
		eval{ $typeAratio=$typeACnt/($typeACnt+$bothCount+$typeBCnt) };
		$typeAratio = $typeACnt."/".($typeACnt+$bothCount+$typeBCnt) if $@;

		my $typeBratio;
		eval{ $typeBratio=$typeBCnt/($typeACnt+$bothCount+$typeBCnt) };
		$typeBratio = $typeBCnt."/".($typeACnt+$bothCount+$typeBCnt) if $@;

		print "$chr\t$currPosition\t$typeACnt\t$bothCount\t$typeBCnt\t".$homoHeter."\t".$typeAratio."\t".$typeBratio."\n";
		
		$currPosition+=$step_size;
		$windowTail=$currPosition+$window_size-1;
	}
}
