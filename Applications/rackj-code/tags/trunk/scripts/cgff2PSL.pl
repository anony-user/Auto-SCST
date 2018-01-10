#!/usr/bin/perl -w

$usage = "Usage: cgff2PSL.pl strandCGFF\n";
$strandCGFF = shift or die $usage;

# read strandCGFF to build geneInfoHOA and cgffHOA
%geneInfoHOA = ();
%cgffHOA = ();
open(FILE,"<$strandCGFF");
while( $line = <FILE> )
{
	@token = split(/\t/, $line);

	if( $line=~/^>/ )
	{
		$geneID = substr $token[0],1;

		# avoiding repeat entries of the same gene
		@{ $geneInfoHOA{$geneID} } = ();
		@{ $cgffHOA{$geneID} } = ();

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

# write PSL
for $geneID (keys %geneInfoHOA){
	# 0: matches
	my $length = 0;
	for(my $idx = 0 ; $idx < @{$cgffHOA{$geneID}} ; $idx += 2){
		$length += (${$cgffHOA{$geneID}}[$idx+1] - ${$cgffHOA{$geneID}}[$idx] + 1);
	}
	# 1: misMatches
	# 2: repMatches
	# 3: nCount
	# 4: qNumInsert
	# 5: qBaseInsert
	# 6: tNumInsert
	# 7: tBaseInsert
	# 8: strand
	my $strand = $geneInfoHOA{$geneID}[1];
	# 9: qName
	# 10: qSize
	# 11: qStart
	# 12: qEnd
	# 13: tName
	my $chr = $geneInfoHOA{$geneID}[0];
	# 14: tSize
	# 15: tStart
	my $tStart = ${$cgffHOA{$geneID}}[0]-1;
	# 16: tEnd
	my $tEnd = ${$cgffHOA{$geneID}}[@{$cgffHOA{$geneID}}-1];
	# 17: blockCount
	my $blockCount = @{$cgffHOA{$geneID}}/2;
	# 18: blockSizes
	my $blockSizes = "";
	for(my $idx = 0 ; $idx < @{$cgffHOA{$geneID}} ; $idx += 2){
		$blockSizes .= (${$cgffHOA{$geneID}}[$idx+1] - ${$cgffHOA{$geneID}}[$idx] + 1);
		$blockSizes .= ",";
	}
	# 19: qStarts
	my $qStarts = "";
	my $qStartPos = 0;
	for(my $idx = 0 ; $idx < @{$cgffHOA{$geneID}} ; $idx += 2){
		$qStarts .= $qStartPos;
		$qStarts .= ",";
		
		$qStartPos += (${$cgffHOA{$geneID}}[$idx+1] - ${$cgffHOA{$geneID}}[$idx] + 1);
	}
	# 20: tStarts
	my $tStarts = "";
	for(my $idx = 0 ; $idx < @{$cgffHOA{$geneID}} ; $idx += 2){
		$tStarts .= (${$cgffHOA{$geneID}}[$idx] - 1);
		$tStarts .= ",";
	}

	print "$length\t0\t0\t0\t0\t0\t0\t0\t$strand\t$geneID\t$length\t0\t$length\t$chr\t0\t$tStart\t$tEnd\t$blockCount\t$blockSizes\t$qStarts\t$tStarts\n";
}

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}
