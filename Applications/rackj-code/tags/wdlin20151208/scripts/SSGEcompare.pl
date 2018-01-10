#!/usr/bin/perl -w
use strict;
use List::Util qw(sum max);
use Getopt::Long;


my $usage = "Usage: SSGEcompare.pl [ -min <chiSqr threshold> ] <output filename> <CGFF> <fineSplice 1> <geneCoverage 1> <readLen 1> <name 1> ... <fineSplice N> <geneCoverage N> <readLen N> <name N>\n";

my $threshold=3;
GetOptions ('min=f' => \$threshold) or die $usage;

my $output = shift or die $usage;
my $cgff = shift or die $usage;

my $numGroups = @ARGV/4;
my @fineSpliceFile;
my @coverageFile;
my @readLengths;
my @names;
my $withNovelColumn = 0;

die $usage if @ARGV%4>0 or @ARGV==0;

for my $i (0..$numGroups-1) {
	$fineSpliceFile[$i] = shift;
	$coverageFile[$i] = shift;
	$readLengths[$i]=shift;
	$names[$i]=shift;
}

my @fileNameArr;
push(@fileNameArr, @fineSpliceFile);
push(@fileNameArr, @coverageFile);
for(my $i = 0; $i < @fileNameArr; $i++) {
	die  "Absent data file: $fileNameArr[$i]\n" if (!-e "$fileNameArr[$i]") ;
}

my %cgffHoA;
my %patternHash;
my @fineSpliceAoH;
my @coverageAoH;

# handle CGFF
open(CGFF,"<$cgff");
my $gene;
while (<CGFF>) {
	s/\s+$//;
	if($_=~/^>/){
		my @token=split(/\s+/);
		$gene = substr($token[0],1);
	}else{
		my @regions = split;
		push @{$cgffHoA{$gene}}, @regions;
	}
}
close(CGFF);


# handle fineSplice files
for (my $j=0; $j<@fineSpliceFile; $j++) {
	my %spliceHash;
	open(FS,"<$fineSpliceFile[$j]");
	while (<FS>) {
		s/\s+$//;
		next if /^#/;
		my @token=split(/\s+/);
		next until $token[1] =~ /\d+\[\d+-\d+\]/;
		$spliceHash{$token[0]}{$token[1]}=$token[2];
		
		my $novel = 0;
		if($token[3] eq "V"){
                        $novel=1;
                        $withNovelColumn = 1;
		}
		$patternHash{$token[0]}{$token[1]}=$novel if(!exists $patternHash{$token[0]}{$token[1]});
	}
	push(@fineSpliceAoH,\%spliceHash);
	close(FS);
}


# handle geneCoverage files
for(my $j=0; $j<@coverageFile; $j++) {
	
	my %coverageHash;
	open(COV,"<$coverageFile[$j]");
	while(<COV>){
		s/\s+$//;
		next if /^\#/;
		my @token=split(/\t/);
		my $geneID = $token[0];
		my $start = $token[2];
		my $readCnt = $token[3];
		$token[4] =~ /\[(.+)\]/;
		my @coverageArray = split(/\,\s/, $1);
		
		for my $pattern ( keys %{$patternHash{$geneID}} ) {
			my ($exonNum, $posA, $posB) = ($pattern =~ /(\d+)\[(\d+)-(\d+)\]/);
			my $relativeStart = $cgffHoA{$geneID}[($exonNum-1)*2] - $start;
			my $relativeEnd = $cgffHoA{$geneID}[($exonNum-1)*2+1] - $start;
			
			if(($posB-$posA)==1){ # 1bp  
			        delete $patternHash{$geneID}{$pattern};
                                next;
			}
			
			my $e1 = sum @coverageArray[$relativeStart..($relativeStart+$posA)];
			my $I = sum @coverageArray[($relativeStart+$posA+1)..($relativeStart+$posB-1)];
			my $e2 = sum @coverageArray[($relativeStart+$posB)..$relativeEnd];
			
			my $spliceReadCnt = 0;
			$spliceReadCnt = $fineSpliceAoH[$j]{$geneID}{$pattern} if (exists $fineSpliceAoH[$j]{$geneID}{$pattern});
			
			push @{$coverageHash{$geneID}{$pattern}}, $readCnt, ($I/$readLengths[$j]), (($e1+$e2)/$readLengths[$j]), $spliceReadCnt;
		}
	}
	
	for my $gene ( keys %patternHash ){
		for my $pattern ( keys %{$patternHash{$gene}} ){
			if(!exists $coverageHash{$gene}{$pattern}){
				my $spliceReadCnt = 0;
				$spliceReadCnt = $fineSpliceAoH[$j]{$gene}{$pattern} if (exists $fineSpliceAoH[$j]{$gene}{$pattern});
				push @{$coverageHash{$gene}{$pattern}}, 0, 0, 0, $spliceReadCnt;
			}
		}
	}
	
	push(@coverageAoH,\%coverageHash);
	close(COV);
	
}


# compute
my $delta=0.1;
my @resultSet;
foreach my $gene (sort {$a cmp $b} (keys %patternHash)) {
	foreach my $patternStr (sort {$a cmp $b} (keys %{$patternHash{$gene}})){
		
		my @msg;
		push @msg, $gene;
		push @msg, $patternStr;
		
		if($withNovelColumn){
        		if($patternHash{$gene}{$patternStr}) {
        			push @msg, "";
        		}else{
        			push @msg, "V";
        		}
		}
		
		my ($exonNum, $start, $stop) = ($patternStr =~ /(\d+)\[(\d+)-(\d+)\]/);
		my $exonLen = ($cgffHoA{$gene}[($exonNum-1)*2+1] - $cgffHoA{$gene}[($exonNum-1)*2] +1);
		push @msg, $exonLen;
		
		my $spliceStart=$cgffHoA{$gene}[($exonNum-1)*2]+$start;
		my $spliceEnd=$cgffHoA{$gene}[($exonNum-1)*2]+$stop;
		push @msg, "$spliceStart-$spliceEnd";
		
		my @sigmaIR = ($delta) x @coverageAoH;
		my @sigmaEX = ($delta) x @coverageAoH;
		my @readCounts = ($delta) x @coverageAoH;
		my @spliceReadCounts = ($delta) x @coverageAoH;
		
		for (my $i=0;$i<@coverageAoH;$i++) {
			if(exists $coverageAoH[$i]{$gene}{$patternStr}) {		
				my $readCount = $coverageAoH[$i]{$gene}{$patternStr}[0];
				my $sumI = $coverageAoH[$i]{$gene}{$patternStr}[1];
				my $sumE = $coverageAoH[$i]{$gene}{$patternStr}[2];
				my $spliceReadCount = $coverageAoH[$i]{$gene}{$patternStr}[3];
				
				$sigmaIR[$i] = $sumI if $sumI>0;
				$sigmaEX[$i] = $sumE if $sumE>0;
				$readCounts[$i] = $readCount if $readCount>0;
				$spliceReadCounts[$i] = $spliceReadCount if $spliceReadCount>0;
			}
		}
	
		my $chiSquared_neighborExon = chisquared(\@sigmaIR,\@sigmaEX);
		my $chiSquared_readCnt = chisquared(\@sigmaIR,\@readCounts);
		
		my $chi_splice_cmp1 = chisquared(\@sigmaIR,\@spliceReadCounts);
		my $chi_splice_cmp2 = chisquared(\@spliceReadCounts,\@sigmaIR);
		my $chiSquared_spliceReadCnt = ($chi_splice_cmp1<$chi_splice_cmp2)? $chi_splice_cmp1 : $chi_splice_cmp2;
		
		my @chiList = ($chiSquared_neighborExon, $chiSquared_readCnt, $chiSquared_spliceReadCnt);
		my $maxChi = max @chiList;
		next if ($maxChi < $threshold);
		
		# output message
		for my $value (@sigmaIR){
			$value=0 if($value==$delta);
		}
		for my $value (@sigmaEX){
			$value=0 if($value==$delta);
		}
		for my $value (@readCounts){
			$value=0 if($value==$delta);
		}
		for my $value (@spliceReadCounts){
			$value=0 if($value==$delta);
		}
		push @msg, @sigmaIR;
		push @msg, @sigmaEX;
		push @msg, $chiSquared_neighborExon;
		
		push @msg, @readCounts;
		push @msg, $chiSquared_readCnt;
		
		push @msg, @spliceReadCounts;
		push @msg, $chiSquared_spliceReadCnt;
		
		my $result_msg = join("\t", @msg);
		push(@resultSet,["$result_msg", $chiSquared_neighborExon]);
	}
  
}

# report
open(OUT, ">$output");
my $head="#Gene\tSplice\t";
$head .= "Database\t" if $withNovelColumn;
$head .= "ExonLen\tPosition\t";
foreach my $n (@names){
	$head.="$n\_sumI\t";
}
foreach my $n (@names){
	$head.="$n\_sumE\t";
}
$head.="chiSquared\t";
foreach my $n (@names){
	$head.="$n\_readCnt\t";
}
$head.="chiSquared\t";
foreach my $n (@names){
	$head.="$n\_spliceReadCnt\t";
}
$head.="chiSquared";
print OUT "$head\n";

@resultSet = sort {$b->[1] <=> $a->[1]} @resultSet;
for (@resultSet) {
	my @array= @{$_};
	print OUT "$array[0]\n";
}
close(OUT);



#----------------------------
# subroutine
#----------------------------
sub chisquared {
    my @Xn=@{$_[0]};
    my @Yn=@{$_[1]};
    my $i;
    my $j;
    my $sum1;
    my $sum2;
    my $expect;
    my $chi;
    my $chisqure;
    
    for my $val (@Xn) {
         $sum1 += $val;
    }
    
    for my $val (@Yn) {
         $sum2 += $val;
    }
    
    for my $idx (0 .. @Yn-1) {
         if($sum2 > 0) {
              $expect = $sum1*($Yn[$idx]/$sum2);
         }else{
              $expect = 0;
         }
    
         if($expect > 0) {
              $chi = ($Xn[$idx]-$expect)*($Xn[$idx]-$expect) / $expect;
              $chisqure += $chi;
         }else{
              $chisqure = "N/A";
              last;
         }
    }
    return $chisqure;
}
