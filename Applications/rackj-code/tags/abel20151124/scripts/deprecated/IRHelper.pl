#!/usr/bin/perl

# For doing wet-lab validation for IR events, it is much helpful if coordinates of
# covered intron regions of given IR events are know. This program is made for
# automatically computing these coordinate.
#
# Suppose that data files for ReadViz were made by RegionTracer or GeneTracer for
# sampleNames. This program reads a two column inputFileName, where the first column
# for genes and the second columns for corresponding introns. This program reports
# outputFileName with intron boundries and covered intervals.

my $usage = "Usage: IRHelper.pl inputFileName outputFileName sampleNames ... \n";

$numArgs = @ARGV;
if($numArgs < 3)
{
	print $usage;
	exit;
}

$inputFileName = $ARGV[0];
$outputFileName = $ARGV[1];

foreach $argnum (0 .. $#ARGV) 
{
	if( $argnum > 1 )
	{
		$sampleNameArr[$i] = $ARGV[$argnum];
		$i++;
	}
}

$sampleNameArrSize = @sampleNameArr;

%geneIdHash = ();
open(file,"<$inputFileName");
while( $line = <file> )
{
	@token = split(/\t/, trim($line));
	$geneIdHash{$token[0], $token[1]} = "" ;
}
close file;

%intronIntervalHash = ();
for $geneId (sort keys %geneIdHash)
{
	@splitArr = split($;, $geneId);
	for($i = 0; $i < $sampleNameArrSize; $i++)
	{
		$fileName = "$sampleNameArr[$i]".".$splitArr[0]";
		open(file,"<$fileName");
		while( $line = <file> )
		{
			@token = ();
			@token = split(/\t/, trim($line));
			if( $splitArr[1] == $. )
			{
				$intronBegin = $token[3]+1;
			}
			
			if( ($splitArr[1] + 1) == $. )
			{
				$intronEnd = $token[2]-1;
				$intronIntervalHash{$splitArr[0], $splitArr[1]} = "$intronBegin\t$intronEnd";
				last;
			}	
		}
		close file;
	} 
}

%readIntervalHoh = ();
@splitArr =();
for $key (sort keys %intronIntervalHash)
{
	@splitArr = split($;, $key);
	@Interval = split(/\t/, $intronIntervalHash{$key});
	for($i = 0; $i < $sampleNameArrSize; $i++)
	{
		$fileName = "$sampleNameArr[$i]".".$splitArr[0]";
		open(file,"<$fileName");
		while( $line = <file> )
		{
			if( $line =~ /^splice|^uniq/ )
			{
				@token = ();
				@token = split(/\t/, trim($line));
				if( $Interval[0] < $token[2] && $token[2] < $Interval[1])
				{
					if( $token[3] > $Interval[1] )
					{
						$readIntervalHoh{ $splitArr[0], $splitArr[1] }{ $token[2], $Interval[1] } = "";
					}
					else
					{
						$readIntervalHoh{ $splitArr[0], $splitArr[1] }{ $token[2], $token[3] } = "";
					}
				}
				elsif( $Interval[0] < $token[3] && $token[3] < $Interval[1] )
				{
					$readIntervalHoh{ $splitArr[0], $splitArr[1] }{ $Interval[0], $token[3] } = "";
				}
				elsif( $token[2] <= $Interval[0] && $Interval[1] <= $token[3] )
				{
					$readIntervalHoh{ $splitArr[0], $splitArr[1] }{ $Interval[0], $Interval[1] } = "";
					last;
				}
			}	
		}
		close file;
	} 
}

%covereIntervalHoh = ();
for $key ( sort keys %readIntervalHoh ) 
{
	$i = 0;
#	print "$key\t";
	for $Interval ( sort byCoordinate keys %{ $readIntervalHoh{$key} } ) 
	{ 
		$i++;
		@token= ();
		@token = split($;, $Interval);
		if( $i == 1 || ($covereIntervalHoh{$key}{$start1} + 1) < $token[0] )
		{
			$start1 = $token[0];
			$covereIntervalHoh{$key}{$token[0]} = $token[1];
		}
		else
		{
			if( $covereIntervalHoh{$key}{$start1} < $token[1] )
			{
				$covereIntervalHoh{$key}{$start1} = $token[1];	
			}
		}
#		print "$token[0]=$token[1]\t"; 
	}
#	print "\n";
} 

open(STDOUT,">$outputFileName");
print "GeneID\tIntronNo\tIntronStart\tIntronStop\tCovered Intervals\n";
@token = ();
for $key ( sort keys %intronIntervalHash ) 
{
	@token = split($;, $key);
	print "$token[0]\t$token[1]\t$intronIntervalHash{$key}\t";
	for $start ( sort keys %{ $covereIntervalHoh{$key} } )	
	{	
		print "($start, $covereIntervalHoh{$key}{$start}) ";
	}
	print "\n";	
}
close STDOUT;

sub byCoordinate {
	my @split1 = split($;, $a);
	my @split2 = split($;, $b);
	my $x_a = $split1[0];
	my $y_a = $split1[1];
	my $x_b = $split2[0];
	my $y_b = $split2[1];
	
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