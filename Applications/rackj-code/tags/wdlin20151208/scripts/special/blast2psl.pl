#!/usr/bin/perl -w

# translate BLASTP/BLASTN alignments into PSLX format
# translate TBLASTN alignments into PSL format

my $debug = 0;

my $switchQT = 0;
my $oneBlockOut = 0;
my $mode = "";

#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-switchQT') {
		$switchQT=1;
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-oneBlock') {
		$oneBlockOut=1;
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-mode') {
		$mode=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

#
my $queryID;
my $queryLen;
my $targetID;
my $targetLen;
my $strand;
my $hspQstart=0;
my $hspTstart;
my $hspQseq;
my $hspTseq;

# parameter check
if(length($mode)==0){
	die "-mode must be assigned\n";
}elsif($mode eq "blastp"){
	$strand = "+";
}elsif($mode eq "blastn"){
	# do nothing
}elsif($mode eq "tblastn"){
	# do nothing
}else{
	die "incorrect -mode parameter\n";
}


while(<>){
	if(/Query= (\S+)/){
		my $queryIDnew = $1; # save ID
		
		# fire HSP if any
		if($hspQstart!=0){
			# fire an HSP
			hspProc($queryID,$queryLen,$targetID,$targetLen,$strand,uc($hspQseq),uc($hspTseq),$hspQstart,$hspTstart);
		
			# reset hsp to avoid miss fire
			$hspQstart = 0;
		}
		
		$queryID = $queryIDnew; # get ID
		$_ = <>;
		s/,//g;
		/\((\d+)/;
		$queryLen = $1
	}
	if(/^>(\S+)/){
		my $targetIDnew = $1; # save ID
		
		# fire HSP if any
		if($hspQstart!=0){
			# fire an HSP
			hspProc($queryID,$queryLen,$targetID,$targetLen,$strand,uc($hspQseq),uc($hspTseq),$hspQstart,$hspTstart);
		
			# reset hsp to avoid miss fire
			$hspQstart = 0;
		}
		
		$targetID = $targetIDnew; # get ID
		$_ = <>;
		/Length = (\d+)/;
		$targetLen = $1;
	}
	if(/Identities/){
		$hspQstart = 0;
		$hspTstart = 0;
		$hspQseq = "";
		$hspTseq = "";
	}

	# strand, blastn
	if(/Strand = (\S+) \/ (\S+)/ && $mode eq "blastn"){
		$strand = "+";
		$strand = "-" if $1 ne $2;
	}
	# strand, tblastn
	if(/Frame = (.)\d/ && $mode eq "tblastn"){
		$strand = $1;
	}

	if(/Query:\s+(\d+)\s*?(\S+)\s+\d+/){
		$hspQstart = $1 if $hspQstart==0;
		$hspQseq .= $2;
	}
	if(/Sbjct:\s+(\d+)\s*?(\S+)\s+\d+/){
		$hspTstart = $1 if $hspTstart==0;
		$hspTseq .= $2;
	}

	if(/Lambda/ || /Score/ || eof){
		# skip hit 1st Score w/o right hsp info & double Lambda tag
		next if $hspQstart == 0;
		
		# fire an HSP
		hspProc($queryID,$queryLen,$targetID,$targetLen,$strand,uc($hspQseq),uc($hspTseq),$hspQstart,$hspTstart);
		
		# reset hsp to avoid miss fire
		$hspQstart = 0;
	}
}


sub hspProc {
	my ($queryName,$queryLength,$targetName,$targetLength,$strand)=@_[0,1,2,3,4];
	my ($qStr,$tStr,$qStart,$tStart)=@_[5,6,7,8];

	# special prepatation, tblastn
	if($mode eq "tblastn"){
		$queryLength *= 3;
		$qStart = ($qStart*3)-2;

		my @newStrArr;
		for $tmpStr ($qStr,$tStr){
			my $newStr="";
			for(my $idx=0;$idx<length($tmpStr);$idx++){
				$newStr .= (substr($tmpStr,$idx,1)x3);
			}
			push @newStrArr,$newStr;
		}
		($qStr,$tStr) = @newStrArr;
	}
	
	my @alnArray;
	my $multiplier = 1;
	$multiplier = -1 if $strand eq "-";

	my $strLength = length($qStr);
	my @gapIntervals;
	# search query/target side gaps
	while($qStr =~ /-+/g) {
		# query side gap start/end
		push(@gapIntervals,[$-[0],$+[0]-1]);
	}
	while($tStr =~ /-+/g) {
		# target side gap start/end
		push(@gapIntervals,[$-[0],$+[0]-1]);
	}
	
	# iterate per block , number of block equals gap number+1
	my $intervalStart=0;
	my ($qShift,$tShift)=(0,0);
	@gapIntervals = sort IntervalSort @gapIntervals;
	
	for (my $i=0; $i <= @gapIntervals;$i++) {
		my ($blockStart,$blockEnd); # 0-base, in alignment, relative position
		if ($i < @gapIntervals) {
			($blockStart,$blockEnd)=($intervalStart,$gapIntervals[$i][0]-1);
		}else{
			($blockStart,$blockEnd)=($intervalStart,$strLength-1);
		}
		my ($matchCnt,$mismatchCnt)=(0,0);
		my $blockLength = $blockEnd-$blockStart+1;
		
		if($blockLength>0){

			## BLOCK START/END IN ALIGNMENT
			# 1-base, []
			my $qBlockStart1 = $qStart + $blockStart - $qShift;
			my $qBlockEnd1 = $qBlockStart1 + $blockLength - 1;
			my $tBlockStart1 = $tStart + $multiplier*($blockStart - $tShift);
			my $tBlockEnd1 = $tBlockStart1 + $multiplier*($blockLength - 1);
			if($strand eq "-"){
				# switch target side start/end
				my $tmp = $tBlockStart1;
				$tBlockStart1 = $tBlockEnd1;
				$tBlockEnd1 = $tmp;
				# adjust query side start/end to reversed
				$qBlockStart1 = $queryLength - $qBlockEnd1 + 1;
				$qBlockEnd1 = $qBlockStart1 + $blockLength - 1;
			}
			# 0-base, [)
			my $qBlockStart0 = $qBlockStart1-1;
			my $qBlockEnd0   = $qBlockEnd1;
			my $tBlockStart0 = $tBlockStart1-1;
			my $tBlockEnd0   = $tBlockEnd1;
	
			## ALIGNMENT START/END IN SEQ
			my $qAlignStart = $qBlockStart0;
			my $qAlignEnd   = $qBlockEnd0;
			my $tAlignStart = $tBlockStart0;
			my $tAlignEnd   = $tBlockEnd0;
			if($strand eq "-"){
				$qAlignStart = $queryLength - $qBlockEnd0;
				$qAlignEnd = $queryLength - $qBlockStart0;
			}
	
			## SEQs		
			my $qBlockStr = substr($qStr,$blockStart,$blockLength);
			my $tBlockStr = substr($tStr,$blockStart,$blockLength);
			if($strand eq "-" && $mode eq "blastn"){ # reverse-complement both sequences
				$qBlockStr =~ tr/ACGTacgt/TGCAtgca/;
				$tBlockStr =~ tr/ACGTacgt/TGCAtgca/;
				
				$qBlockStr = reverse $qBlockStr;
				$tBlockStr = reverse $tBlockStr;
			}
			
			my @qBlockStrAr = split(//,$qBlockStr);
			my @tBlockStrAr = split(//,$tBlockStr);
			# count sequence match/mismatch
			for (my $i=0; $i<$blockLength; $i++) {
				if ($qBlockStrAr[$i] eq $tBlockStrAr[$i]) {
					$matchCnt++;
				}elsif ($qBlockStrAr[$i] ne $tBlockStrAr[$i]) {
					$mismatchCnt++;
				}
			}

			my @pslxFormatAr;
			# num_match, num_mismatch
			@pslxFormatAr[0,1]=($matchCnt,$mismatchCnt);
			# repMatches,nCount,qNumInsert,qBaseInsert,tNumInsert,tBaseInsert,strand
			@pslxFormatAr[2,3,4,5,6,7,8]=("0","0","0","0","0","0",$strand);
			# qName,qSize,qStart,qEnd
			@pslxFormatAr[9,10,11,12] = ($queryName,$queryLength,$qAlignStart,$qAlignEnd);
			# tName,tSize,tStart,tEnd
			@pslxFormatAr[13,14,15,16] = ($targetName,$targetLength,$tAlignStart,$tAlignEnd);
			# blockCount,blockSizes,qStarts,tStarts
			@pslxFormatAr[17,18,19,20] = (1,"$blockLength,","$qBlockStart0,","$tBlockStart0,");
			# qStr, tStr
			@pslxFormatAr[21,22] = ("$qBlockStr,","$tBlockStr,") if $mode ne "tblastn";

			push(@alnArray, join("\t",@pslxFormatAr));
		}	
		last if $i == @gapIntervals;
		#update for next alignment block
		$intervalStart = $gapIntervals[$i][1]+1;
		# calc gap shift for next alignmentBlock
		$qShift += $gapIntervals[$i][1]-$gapIntervals[$i][0]+1 if substr($qStr,$gapIntervals[$i][0],1) eq "-";
		$tShift += $gapIntervals[$i][1]-$gapIntervals[$i][0]+1 if substr($tStr,$gapIntervals[$i][0],1) eq "-";
	}
	
	if($switchQT){
	        # each element of @alnArray is a one-block PLS record
	        for(my $idx=0; $idx<@alnArray; $idx++){
	                $alnArray[$idx] = pslQtSwitch($alnArray[$idx]);
	        }
        }

        # reverse this one-block-alignment array if necessary
        if($strand eq "-" && $switchQT==0){
        	@alnArray=reverse(@alnArray);
	}

	# output
	if($oneBlockOut){
		for my $alnStr (@alnArray){
			print "$alnStr\n";
		}
	}else{
		# merge one-block-alignments
		print pslMerge(\@alnArray)."\n";
	}
}

sub IntervalSort {
	my @aStartEnd = @{$a};
	my @bStartEnd = @{$b};
	return $aStartEnd[0] <=> $bStartEnd[0] || $aStartEnd[1] <=> $bStartEnd[1];
}

sub pslQtSwitch {
        my $pslLine = shift;
        $pslLine =~ s/^\s+|\s+$//g;
	my @pslxAr = split(/\t/,$pslLine);
	# strand
	my $strand = $pslxAr[8];
	# qName,qSize,qStart,qEnd vs tName,tSize,tStart,tEnd
	my ($queryName,$queryLength,$qAlignStart,$qAlignEnd) = @pslxAr[9,10,11,12];
	my ($targetName,$targetLength,$tAlignStart,$tAlignEnd) = @pslxAr[13,14,15,16];
	# numBlocks,qStarts,tStarts
	my ($numBlocks,$qBlockStartStr,$tBlockStartStr) = @pslxAr[17,19,20];
	$pslxAr[9]  = $targetName;
	$pslxAr[10] = $targetLength;
	$pslxAr[11] = $tAlignStart;
	$pslxAr[12] = $tAlignEnd;
	$pslxAr[13] = $queryName;
	$pslxAr[14] = $queryLength;
	$pslxAr[15] = $qAlignStart;
	$pslxAr[16] = $qAlignEnd;
	if($strand eq "+"){
	        # just switch if forward alignment
	        $pslxAr[19] = $tBlockStartStr;
		$pslxAr[20] = $qBlockStartStr;
	}else{  
		$pslxAr[19] = ($targetLength-$tAlignEnd).",";
		$pslxAr[20] = "$qAlignStart,";
	}	
	# qStr, tStr
	if(@pslxAr>21){
		my ($qBlockStr) = $pslxAr[21]=~/(.+?),/;
		my ($tBlockStr) = $pslxAr[22]=~/(.+?),/;
		if($strand eq "+"){
                        # just switch if forward alignment
		        $pslxAr[21] = "$tBlockStr,";
		        $pslxAr[22] = "$qBlockStr,";
		}else{
         	        # reverse-complement both sequences
		        $tBlockStr =~ tr/ACGTacgt/TGCAtgca/;
		        $qBlockStr =~ tr/ACGTacgt/TGCAtgca/;
		        $pslxAr[21] = reverse($tBlockStr).",";
		        $pslxAr[22] = reverse($qBlockStr).",";
	        }
	}	
	return join("\t",@pslxAr);
}

sub pslMerge {
        my $pslRecords_ref = shift;
        my @pslMergeRecord;
        my @pslxAr;
        for (my $i=0; $i < @{$pslRecords_ref};$i++) {
                @pslxAr = split(/\t/,${$pslRecords_ref}[$i]);
                if ($i == 0){
                        # @pslMergeRecord is undefined
                        for (my $j=0; $j < @pslxAr;$j++){
                                 push(@pslMergeRecord,$pslxAr[$j]);
                        }
                }else{
                        # just handle someone where there is changed
                        # num_match, num_mismatch
                        $pslMergeRecord[0]+=$pslxAr[0];
                        $pslMergeRecord[1]+=$pslxAr[1];
                        # qStart,qEnd
                        $pslMergeRecord[11]=$pslxAr[11] if ($pslMergeRecord[11] > $pslxAr[11]);
                        $pslMergeRecord[12]=$pslxAr[12] if ($pslMergeRecord[12] < $pslxAr[12]);
                        # tStart,tEnd
                        $pslMergeRecord[15]=$pslxAr[15] if ($pslMergeRecord[15] > $pslxAr[15]);         
                        $pslMergeRecord[16]=$pslxAr[16] if ($pslMergeRecord[16] < $pslxAr[16]);
                        # blockCount,blockSizes,qStarts,tStarts
                        $pslMergeRecord[17]+=$pslxAr[17];
                        $pslMergeRecord[18].=$pslxAr[18];
                        $pslMergeRecord[19].=$pslxAr[19];
                        $pslMergeRecord[20].=$pslxAr[20];
                        # qStr, tStr
                        if(@pslxAr>21){
	                        $pslMergeRecord[21].=$pslxAr[21];
	                        $pslMergeRecord[22].=$pslxAr[22];
                        }
                }
        }
        
        return join("\t",@pslMergeRecord);
}
