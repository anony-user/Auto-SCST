#!/usr/bin/perl

# this program reads two .geneCoverage files, then, for each gene,
# computes the maximum distance between transcription start(stop) points.

my $usage = "Usage: CoveredRegionDiff.pl <outputFilename> <strandCGFF> <prefix1> <prefix2>\n";

$numArgs = @ARGV;
if($numArgs != 4)
{
	print $usage;
	exit;
}

$outputFilename = $ARGV[0];
$CGFF = $ARGV[1];
foreach $argnum (0 .. $#ARGV) 
{
	if( $argnum > 1 )
	{
		$geneCoverageArr[$i] = $ARGV[$argnum].".geneCoverage";
		$i++;
	}
}
$geneCoverageArrSize = @geneCoverageArr;
push(@inputFile, $CGFF);
$inputFileNumSize = push(@inputFile, @geneCoverageArr);

for($i = 0; $i < $inputFileNumSize; $i++)
{
	if(-e "$inputFile[$i]"){
	}
	else{
		print "Absent data file, check for fileName: $inputFile[$i]\n";
		exit;
	}
}

%KeySetHash = ();
for($i = 0; $i < $geneCoverageArrSize; $i++)
{
	open(file,"<$geneCoverageArr[$i]");
	while($line = <file>)
	{
		@token= split(/\t/, $line);
		if($. > 1)
		{	
			$KeySetHash{$token[0]} = $token[1];
		}
	}
 	close file;
}

%strandHash = ();
%exonPositHoA = ();
open(file,"<$CGFF");
while($line = <file>)
{	
	@token = ();
	@token = split(/\t/, trim($line));
	if( $line =~ /^>/ ) 
	{
		if($counter > 0)
		{
			$exonPositHoA{$geneId} = [ @PositionArr ];
		}
		@spiltId = split(/^>/, $token[0]);
		$geneId = trim(@spiltId[1]);
		$strandHash{$geneId} = $token[4];
		$counter = 0;
		@PositionArr = ();
		$dataLine = "";
	}
	
	else
	{
		if($counter == 0)
		{
			$cgffExonFirstStart = $token[0];
			$switchGeneStart = $token[0] - $token[0];
			$switchGeneStop = $token[1] - $token[0];
			$dataLine = "$switchGeneStart\t$switchGeneStop";
		}
		
		$counter = 1;
		if($counter > 0)
		{
			$switchGeneStart = $token[0] - $cgffExonFirstStart;
			$switchGeneStop = $token[1] - $cgffExonFirstStart;
			$dataLine = "$switchGeneStart\t$switchGeneStop";
		}
		push(@PositionArr,  $dataLine);
#		push(@PositionArr,  $switchGeneStop);	
	}
}
close file;
		
if($counter > 0)
{
	$exonPositHoA{$geneId} = [ @PositionArr ];
}

#for $family ( keys %exonPositHoA ) 
#{ 
#	for $i ( 0 .. $#{ $exonPositHoA{$family} } ) 
#	{ 
#		print "$family\t$exonPositHoA{$family}[$i]\n"; 
#	} 
#} 

@geneCoverageAoH= ();
for($i = 0; $i < $geneCoverageArrSize; $i++)
{
	open(file,"<$geneCoverageArr[$i]");
	$rec = {};
	@token = ();
	while($line = <file>)
	{
		if($. > 1)
		{   
			@token1 = ();
			$str = "";
			@token= split(/\t/, $line);
			$str = $token[2];
			@token1= split(/[\, [\]]+/, trim($token[3]));
			$token1Size = @token1; 
			@token1 = @token1[1..$token1Size - 1];
			$token1Size = @token1;
#			print "$token1Size\n";

			for($j = 0; $j < $token1Size; $j++)
			{
				if($token1[$j] != 0)
				{
#					print "$counter1\n";
					$counter1 = $j;
					$str = "$str\t$counter1";
					last;
				}
			}
#			print $str."\n";
			for($j = $token1Size-1; $j >=0; $j--)
			{   
				if($token1[$j] != 0)
				{
					$counter2 = $j;
					$str = $str."\t".$counter2;
					last;
				}	
			}
   			$rec -> {$token[0]} = $str;
		}
	}
	push @geneCoverageAoH, $rec;
	close file
}

#for $i ( 0 .. $#geneCoverageAoH ) 
#{ 
#	for $role ( keys %{ $geneCoverageAoH[$i] } )
#	{ 
#		print "$role\t$geneCoverageAoH[$i]{$role}\n"; 
#	} 
#} 

$geneCoverageAohSize = @geneCoverageAoH;
open(STDOUT,">$outputFilename");
print "geneID\tstart\tstrand\t#read$ARGV[2]\t#read$ARGV[3]\tstart$ARGV[2]\tstop$ARGV[2]\tstart$ARGV[3]\tstop$ARGV[3]\tmaxEmpty\tratio\n";

for $key (keys %KeySetHash)
{
	$uniCnt = "";
	$lenIndex = "";
	for($i = 0; $i < $geneCoverageAohSize; $i++)
	{		
		if(exists $geneCoverageAoH[$i]{$key})
		{
#			print "$geneCoverageAoH[$i]{$key}\t";
			@tempArr = split(/\t/, $geneCoverageAoH[$i]{$key});
			$uniCnt = $uniCnt.$tempArr[0]."\t";
			$lenIndex = $lenIndex.$tempArr[1]."\t".$tempArr[2]."\t"; 
		}
		else
		{
			$uniCnt = $uniCnt."0"."\t";
			$lenIndex = $lenIndex."0"."\t"."0"."\t";
		}		
	}
	
	@tempArr = ();
	@tempArr = split(/\t/, $lenIndex);
	$tempArrSize = @tempArr;
	@posArr = ();
	for($i = 0; $i < $tempArrSize; $i++)
	{
		$indx = 0;
		for $j ( 0 .. $#{ $exonPositHoA{$key} } ) 
		{	
			@token = split(/\t/, $exonPositHoA{$key}[$j]);
			$exonStart = $token[0];
			$exonStop = $token[1];
			if($tempArr[$i] <= $exonStart)
			{
				$indx += 1;
				push @posArr, $indx;
				last;
			}
			elsif($tempArr[$i] <= $exonStop)
			{
				$indx += ($tempArr[$i] - $exonStart + 1);
				push @posArr, $indx; 
				last;
			}
			else
			{
				$indx += ($exonStop - $exonStart + 1);
			}			
#			print "$exonPositHoA{$key}[$j]\t";
		}
	}
	
	@sortedPosArr = sort {$a <=> $b} @posArr;
	@distArr = ($posArr[0]-$sortedPosArr[0],$posArr[2]-$sortedPosArr[0],
	            $sortedPosArr[3]-$posArr[1],$sortedPosArr[3]-$posArr[3]);
	@sortedDistArr = sort {$a <=> $b} @distArr;

	print "$key\t$KeySetHash{$key}\t$strandHash{$key}\t$uniCnt".
	      "$posArr[0]\t$posArr[1]\t$posArr[2]\t$posArr[3]\t$sortedDistArr[3]\t".
	      $sortedDistArr[3]/($sortedPosArr[3]-$sortedPosArr[0])."\n";
}
close STDOUT;

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}