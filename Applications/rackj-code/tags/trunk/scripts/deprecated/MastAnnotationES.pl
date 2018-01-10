#!/usr/bin/perl
use Bio::DB::Fasta;

$usage = "Usage: MastAnnotationES.pl strandCFFF esListFile width mastIn fastaIn genomeFastaIn outFilename\n";
$strandCGFF = shift or die $usage;
$esListFile = shift or die $usage;
$width = shift or die $usage;
$mastIn = shift or die $usage;
$fastaIn = shift or die $usage;
$genomeFastaIn = shift or die $usage;
$outFilename = shift or die $usage;

# read strandCGFF to build geneInfoHOA and cgffHOA
%geneInfoHOA = ();
%cgffHOA = ();

open(FILE,"<$strandCGFF") or die "Cant't not open $strandCGFF:$!";
while( $line = <FILE> )
{	
	@token = split(/\t/, $line);
	$geneID;

	if( $line=~/^>/ )
	{
		$geneID = substr $token[0],1;
		if(@token>4){
			# chr in array[0] and strand in array[1]
			push @{ $geneInfoHOA{$geneID} },trim($token[1]),trim($token[4]); 
		}else{
			die "$strandCGFF not a strand CGFF\n"
		}
	}else{
		push @{ $cgffHOA{$geneID} },trim($token[0]),trim($token[1]); 
	}
}
close FILE;


# read exons in eaList file into exonHash
%exonHash = ();
open(FILE,"<$esListFile") or die "Cant't not open $esListFile:$!";
while( $line = <FILE> )
{
	@token = split(/\s/, $line);
	$exonHash{$token[0],$token[1]}="";
}
close FILE;


# build lookupHOA
%lookupHOA = ();
for $exon ( sort byExon keys %exonHash )
{
	# getting exon infor
	@token = split($;, $exon);
	$geneID = $token[0];
	$exonPair = $token[1];
	@splitExon = split(/<=>/, $exonPair);
	# 
	$startPosition = $cgffHOA{$geneID}[2*($splitExon[0]-1)+1];
	$stopPosition = $cgffHOA{$geneID}[2*$splitExon[1]-2];
	# get chr and strand
	$chromosome = $geneInfoHOA{$geneID}[0];
	$strand = $geneInfoHOA{$geneID}[1];
	
	if($strand eq "+"){
		$lookupString = "$geneID"."_".$splitExon[0]."_".$splitExon[1]."_"."forward";
		push @{ $lookupHOA{$lookupString} },$geneID,$splitExon[0],$splitExon[1],$chromosome,$strand,($startPosition-$width),($stopPosition+$width); 
	}else{
		$lookupString = "$geneID"."_".$splitExon[0]."_".$splitExon[1]."_"."reverse";
		push @{ $lookupHOA{$lookupString} },$geneID,$splitExon[0],$splitExon[1],$chromosome,$strand,($stopPosition+$width),($startPosition-$width);
	}
}


# for each line in mastIn file
$fastaDB = Bio::DB::Fasta->new($fastaIn);
$genomeDB = Bio::DB::Fasta->new($genomeFastaIn);

open(FILE,"<$mastIn") or die "Cant't not open $mastIn:$!";
open(OUT,">$outFilename");
print OUT "sequence_name\tmotif\thit_start\thit_end\tscore\thit_p-value\thit_region\thitStart\thitStop\trelativePos\tintervals\n";
while( $line = <FILE> )
{	
  next if ($line =~ /^#/);
	@token = split(/\s+/, $line);
	$lookupString = $token[0];
	$hitStart = $token[2];
	$hitStop = $token[3];
	
	# get corresponding infor
	($geneID, $splitExon1, $splitExon2, $chromosome, $strand, $seqStartGenome, $seqStopGenome) = @{ $lookupHOA{$lookupString} };
	
	# position assert
	$chrLength = $genomeDB->length($chromosome);
	$seqStartGenome = assertPosition($seqStartGenome,$chrLength);
	$seqStopGenome = assertPosition($seqStopGenome,$chrLength);
	
	# transfer hitStart & hitStop into hitStartGenome & hitStopGenome
	if($strand eq "+"){
		$hitStartGenome = $seqStartGenome + $hitStart -1;
		$hitStopGenome = $seqStartGenome + $hitStop -1;
	}else{
		$hitStartGenome = $seqStartGenome - $hitStart +1;
		$hitStopGenome = $seqStartGenome - $hitStop +1;
	}
	
	
	# find position of hitStart relative to the intron
	# STEP1: find the intron/exon interval that containing hitStart
	if($strand eq "+"){
		$pointIndex1 = 2*($splitExon1-1);
		$pointIndex2 = 2*$splitExon1-1;
	}else{
		$pointIndex1 = 2*($splitExon2-1);
		$pointIndex2 = 2*$splitExon2-1;
	}
	
	$isExon = 1;
	$pointValue1 = ($isExon ? $cgffHOA{$geneID}[$pointIndex1] : $cgffHOA{$geneID}[$pointIndex1] + 1);
	$pointValue2 = ($isExon ? $cgffHOA{$geneID}[$pointIndex2] : $cgffHOA{$geneID}[$pointIndex2] - 1);
	
	# update positionStr
	if($strand eq "+"){
		$positionStr = "($pointValue1, $pointValue2) ";
	}else{
		$positionStr = "($pointValue2, $pointValue1) ";
	}
	
	#count how mamy times it moves and its direction
	$count = 0;
	while(!between($hitStartGenome,$pointValue1,$pointValue2)){
		# decide the direction to move
		if($hitStartGenome < $pointValue1){
			$direction = -1;
		}else{
			$direction = 1;
		}
		$count += $direction;
		
		# attempt to move
		$pointIndex1 += $direction;
		$pointIndex2 += $direction;
		
		
		if($pointIndex1 < 0 || $pointIndex2 >= @{$cgffHOA{$geneID}}){
			# no more movement
			last;
		}else{
			$isExon = !$isExon;
			$pointValue1 = ($isExon ? $cgffHOA{$geneID}[$pointIndex1] : $cgffHOA{$geneID}[$pointIndex1] + 1);
			$pointValue2 = ($isExon ? $cgffHOA{$geneID}[$pointIndex2] : $cgffHOA{$geneID}[$pointIndex2] - 1);
			if($strand eq "+"){
				$positionStr .= "($pointValue1, $pointValue2) ";
			}else{
				$positionStr .= "($pointValue2, $pointValue1) ";
			}
		}
	}
	
	if($strand eq "-"){
		$count = -$count;
	}
	
	# STEP2: make description of hitStartGenome
	if($pointIdx1 < 0 || $pointIdx2 >= @{$cgffHOA{$geneID}}){
		$positionDesc = "OutRange";
	}else{
		if($count eq 0){
			$positionDesc = "Exon0";
		}elsif($count%2 eq 0){
			if($count > 0){
				$positionDesc = "Exon"."+".($count/2);
			}else{
				$positionDesc = "Exon".($count/2);
			}
		}else{
			if($count > 0){
				$positionDesc = "Intron"."+".( $count + 1 )/2;
			}else{
				$positionDesc = "Intron".( $count - 1 )/2;
			}
		}
	}
	
	foreach (@token){
	  print OUT trim($_)."\t";
	}
	print OUT $fastaDB->seq($lookupString, $hitStart => $hitStop)."\t";
	print OUT "$hitStartGenome\t$hitStopGenome\t$positionDesc\t";
	print OUT "$positionStr\n";
}
close OUT;
close FILE;


sub byExon 
{
	my @token1 = split($;, $a);
	my @token2 = split($;, $b);
	my $x_a = $token1[0];
	my $y_a = $token1[1];
	my $x_b = $token2[0];
	my $y_b = $token2[1];
	if(($x_a cmp $x_b) != 0){
		return ($x_a cmp $x_b);
	}else{
		return ($y_a cmp $y_b);
	}
}

sub trim
{
	my $str = shift;
	$str =~ s/\s+$//g;
	$str =~ s/\s+$//g;
	return $str;
}

sub assertPosition
{
	my $inPos = shift;
	my $max = shift;
	
	if($inPos<1){
		return 1;
	}
	
	if($inPos>$max){
		return $max;
	}
	
	return $inPos;
}

sub between
{
	my $inPos = shift;
	my $point1in = shift;
	my $point2in = shift;

	my $point1 = (($point1in < $point2in)? $point1in : $point2in);
	my $point2 = (($point2in < $point1in)? $point1in : $point2in);
	
	if($point1<=$inPos && $inPos<=$point2){
		return 1;
	}else{
		return 0;
	}
}