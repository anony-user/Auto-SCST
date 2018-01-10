#!/usr/bin/perl 

# this program reads a CGFF file, then, for a certain distance, outputs
# 1. ratio of intergenic distances (between non-overlaping genes)
# 2. ratio of intronic distances (between exons)
# 3. precision for item 2
# 4. corresponding F-measure

my $usage = "Usage: CGFFStatistics.pl <inputCGFF> [<step> <maximum> <outputFileName>]\n";

$numArgs = @ARGV;
if($numArgs < 1)
{
	print $usage;
	exit;
}

my $inputFileName = @ARGV[0];
my $step = @ARGV[1];
my $maximum = @ARGV[2];
my $outputFileName = @ARGV[3];

if(-e "$inputFileName"){
}
else{
	print "Absent data file, check for file: $inputFileName\n";
	exit;
}

if($numArgs > 1)
{
	if($numArgs < 3)
	{
		print "$usage";
		print "maximum not assigned\n";
		exit;
	}
	elsif($numArgs < 4)
	{
		print "$usage";
		print "outputFileName not assigned\n";
		exit;	
	}
	
	if(-e "$outputFileName")
	{
		print "Existing output filename: $outputFileName\n";
		exit;
	}
}

open(file,"<$inputFileName");

$HoHoA = {};
while($line = <file>)
{
	@token = ();
	if($line =~/^>/)
	{
		$intronArrsize = @intronArr;
		
		if($intronArrsize > 0)
		{
			push @exonDistance, @intronArr;
		}
		
		$counter = 0;	
		@intronArr = ();
		$indx = 0;
		
		@token = split(/\t/, $line);	
		@geneID = split(/>/, $token[0]);	
		@position[0] = trim($token[2]);
		@position[1] = trim($token[3]);		
		$HoHoA{ trim($token[1]) }{ trim($geneID[1]) } = [ @position ];
	}
	
	else
	{
		$counter++;
		@token = split(/\t/, $line);
		if( $counter == 1 )
		{
			$start = @token[1];
		}
	
		if( $counter > 1)
		{
			#print "$start\t";
			$ends  = @token[0];
			$len =  $ends - $start - 1;
			if($len > 0)
			{
				@intronArr[$indx] = $len;
				$indx++;
				$sumExonDistance += $len;
			}
			$start = @token[1];
			#print "$ends\n";
		}
	}
}
close file;

$intronArrsize = @intronArr;
if($intronArrsize > 0)
{
	push @exonDistance, @intronArr;
}

$exonDistanceArrSize = @exonDistance;

for $chromosome ( sort keys %HoHoA )
{
	$counter = 0;
	for $geneId ( sort byCoordinate keys %{ $HoHoA{$chromosome} } )
	{
#		print "$chromosome\t$geneId\t@{$HoHoA{$chromosome}{$geneId}}\n";

		$counter++;
		if( $counter == 1 )
		{
			$start1 = $HoHoA{$chromosome}{$geneId}[0];
			$ends1 = $HoHoA{$chromosome}{$geneId}[1];
			$gene1 = $geneId;
		}
		
		if( $counter > 1)
		{
#			print "$start\t";
			$gene2 = $geneId;
			$start2 = $HoHoA{$chromosome}{$geneId}[0];
			$ends2 = $HoHoA{$chromosome}{$geneId}[1];
			$len =  $start2 - $ends1 - 1;
			if($len > 0)
			{
				push @geneDistance, $len;
				$sumGeneDistance += $len;
			}
			$gene1 = $geneId;
			if($ends2 > $ends1){
				$ends1 = $ends2;
			}
		}
	}
}

$geneDistanceArrSize = @geneDistance;

#print $geneDistanceArrSize;
@exonDistance = sort { $a <=> $b } @exonDistance;
@geneDistance = sort { $a <=> $b } @geneDistance;
#print @exonDistance[$exonDistanceArrSize-1];
$geneMinDistance = @geneDistance[0];
$geneMaxDistance = @geneDistance[$geneDistanceArrSize-1];
$geneAverageDistance = $sumGeneDistance/$geneDistanceArrSize;
$geneAverageDistance = sprintf("%.2f", $geneAverageDistance);
$exonMinDistance = @exonDistance[0];
$exonMaxDistance = @exonDistance[$exonDistanceArrSize-1];
$exonAverageDistance = $sumExonDistance/$exonDistanceArrSize;
$exonAverageDistance = sprintf("%.2f", $exonAverageDistance);
print "intergenic interval: "."$geneMinDistance $geneMaxDistance $geneAverageDistance $geneDistanceArrSize\n";
print "intronic interval: ".$exonMinDistance." ".$exonMaxDistance." "."$exonAverageDistance $exonDistanceArrSize\n";
print "(minimum maximum average #interval)\n";

if($numArgs > 1)
{
	open(STDOUT,">$outputFileName");
	print "distance\tintergenicRatio\tintronicRatio\tprecision\tF-measure\n";
	$ind1 = 0;
	$ind2 = 0;
	for( $range = $step; $range <= $maximum; $range += $step )
	{
		print "$range\t";
		while( $counter1 < $geneDistanceArrSize  && $geneDistance[$ind1] <= $range )
		{
			$counter1++;
			$ind1++;
		}
		$intergenicRatio = $counter1/$geneDistanceArrSize;
		print "$intergenicRatio\t";
		
		
		while( $counter2 < $exonDistanceArrSize && $exonDistance[$ind2] <= $range )
		{
			$counter2++;
			$ind2++;
		}
		$introngenicRatio = $counter2/$exonDistanceArrSize;
		print "$introngenicRatio\t";
		
		$pre = precision($intergenicRatio, $introngenicRatio, $geneDistanceArrSize,
			$exonDistanceArrSize);
		print "$pre\t";
		
		$F_m = F_measure($pre, $introngenicRatio);
		print "$F_m\n";
	}
	close STDOUT;
}

sub byCoordinate {
	my $x_a = @{$HoHoA{$chromosome}{$a}}[0];
	my $y_a = @{$HoHoA{$chromosome}{$a}}[1];
	my $x_b = @{$HoHoA{$chromosome}{$b}}[0];
	my $y_b = @{$HoHoA{$chromosome}{$b}}[1];
	
	if($x_a < $x_b){
		return -1;
	}elsif($x_a > $x_b){
		return 1;
	}else{
		if($y_a < $y_b){
			return -1;
		}elsif($y_a > $y_b){
			return 1;
		}else{
			return 0;
		}
	}
}

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}

sub precision($$$$)
{
	my $x1 = shift;
	my $y1 = shift;
	my $amount1 = shift;
	my $amount2 = shift;
	my $result;
	$result = $amount2 * $y1 / ($amount2 * $y1 + $amount1 * $x1);
	return $result;	
}

sub F_measure($$)
{
	my $x1 = shift;
	my $y1 = shift;
	my $result;
	$result = 2 * $x1 * $y1 / ($x1 + $y1);
	return $result;	
}