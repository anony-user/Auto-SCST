#!/usr/bin/perl -w

no warnings qw(once);

# Author: lh3

# This script calculates a score using the BLAST scoring
# system. However, I am not sure how to count gap opens and gap
# extensions. It seems to me that column 5-8 are not what I am
# after. This script counts gaps from the last three columns. It does
# not generate reference skip (N) in the CIGAR as it is not easy to
# directly tell which gaps correspond to introns.

# modified by Peter Tsai and Wen-Dar Lin 20120103
# for implementation of
# 1. SQ header
# 2. precise reflection of BLAT alignment blocks
# 3. NM attributes (#mismatches + #N's)
# 4. MD attributes (String for mismatching positions)

use strict;
use Bio::DB::Fasta;

my $MD=0;
my $ChrChange;
my $minorLimit=0;

#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-u') {
		$ChrChange=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-md') {
		$MD=1;
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-rm') {
		$minorLimit=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if defined $_; }
@ARGV=@new_arg;

die("Usage: psl2sam.pl [-u <chrMappingFile>] [-rm <minorLimit>] [-md] <genomeFasta> <inPSL>\n") if (@ARGV < 2 );
my %chrMap;

if (defined $ChrChange) {
	open(chrDATA, $ChrChange);
	while(<chrDATA>) {
		my @t = split;
		$chrMap{$t[0]}=$t[1];
	}
}

# Retrieve sequence ID & LENGTH from FASTA file
my %fasLength;
my %seqdb if $MD;
my $nullpath = File::Spec->devnull();
open(CPERR, ">&STDERR");
open(STDERR,">$nullpath");
my $stream  = Bio::DB::Fasta->new($ARGV[0])->get_PrimarySeq_stream;
while (my $seq = $stream->next_seq) {
	my $FasID  = $seq->id;
	my $FasLength  = $seq->length;
	$FasID = ${chrMap}{$FasID} if exists ${chrMap}{$FasID};
	$seqdb{$FasID}=$seq->seq() if $MD;
	$fasLength{$FasID}=$FasLength;
}
open(STDERR, ">&CPERR");

open(GENOME,"<$ARGV[0]");
while(my $line=<GENOME>){
	next until $line=~/^>(\S+)/;
	print "\@SQ\tSN:$1\tLN:".$fasLength{$1}."\n";
}
close(GENOME);

# Convert PSL format to SAM format
open(pslDATA, $ARGV[1]) or die("Error: cannot open PSL file '$ARGV[1]'\n");
while (<pslDATA>) {
	next unless (/^\d/);
	
	s/\s+$//g;
	my @t = split(/\t/,$_);
	my @s;
	my $cigar = '';
	# variables for MD tag 
	my $MDstr = '';
	my $NM=0;

	# fix target size if needed -- first try, use old name
	$t[14] = $fasLength{$t[13]} if $t[14]==0;

	# fix target name if needed
	$t[13] = $chrMap{$t[13]} if exists $chrMap{$t[13]};

	# fix target size if needed -- second try, use new name
	$t[14] = $fasLength{$t[13]} if $t[14]==0;

	# for translated BLAT
	my ($first,$second) = ("","");
	if(length($t[8])>1){
		$first = substr($t[8],0,1);
		$second = substr($t[8],1,1);
		if($first eq $second){
			$t[8]='+';
		}else{
			$t[8]='-';
		}
	}

	if ($t[8] eq '-') {  #calculate qStart , qEnd
		my $tmp = $t[11];
		$t[11] = $t[10] - $t[12];
		$t[12] = $t[10] - $tmp;
	}

	my @x = split(',', $t[18]);  # blockSizes
	my @y = split(',', $t[19]);  # qStarts
	my @z = split(',', $t[20]);  # tStarts

	# upper-case qSeqs/tSeqs if needed
	if($MD){
		$t[21] = uc($t[21]) if defined $t[21];
		$t[22] = uc($t[22]) if defined $t[22];
	}
	
	# reverse blockSizes, qStarts, tStarts if $second (target strand) is negative
	if ($second eq "-") {
		@x = reverse(@x);
		@y = reverse(@y);
		@z = reverse(@z);
		for my $i (0 .. $t[17]-1) {
			$y[$i] = $t[10]-$x[$i]-$y[$i];
			$z[$i] = $t[14]-$x[$i]-$z[$i];
		}
		
		# reverse-complement if MD is required
		if($MD){
			if(defined $t[21]){
				$t[21] = reverse(substr($t[21],0,-1)) ;
				$t[21] =~ tr/ATCG/TAGC/;
			}
			if(defined $t[22]){
				$t[22] = reverse(substr($t[22],0,-1)) ;
				$t[22] =~ tr/ATCG/TAGC/;
			}
		}
	}
	
	# set QStrs/TStrs if MD is required
	my @QStrs;
	my @TStrs;
	if($MD){
		if((defined $t[21]) && length($t[21])>0){
			@QStrs = split(",",$t[21]);
		}
		if((defined $t[22]) && length($t[22])>0){
			@TStrs = split(",",$t[22]);
		}
	}

	# blockSizes/qStarts/tStarts adjustment, if needed
	my $beenModified = 0;
	while(my $overlapIdx = overlapCheck(\@x,\@y,\@z)){
		overlapFix(\@x,\@y,\@z, $overlapIdx,\@QStrs,\@TStrs);
		$beenModified = 1;
	}
	if($minorLimit){
		$beenModified = removeMinor(\@x,\@y,\@z,\@QStrs,\@TStrs,$minorLimit);
	}
	if($beenModified){
		# number of blocks
		$t[17] = @x;
		# qStart qEnd
		$t[11] = $y[0];
		$t[12] = $y[@x-1]+$x[@x-1];
		# tStart tEnd
		$t[15] = $z[0];
		$t[16] = $z[@x-1]+$x[@x-1];
	}
	
	@s[0..4] = ($t[9], (($t[8] eq '+')? 0 : 16), $t[13], $t[15]+1, 0);
	@s[6..10] = ('*', 0, 0, '*', '*');

	# decide clip char, H or S
	my $clipChar = "H";
	# set SEQ, if possible
	if((defined $t[23]) && length($t[23])==($t[10]+1)){
		$clipChar = "S"; # modify clipChar if query sequence appended & qualified
		# set SEQ
		if($t[23]=~/^\-/){
			$s[9] = reverse(substr($t[23],1));
			$s[9] =~ tr/ATCGatcg/TAGCtagc/;
		}else{
			$s[9] = substr($t[23],1);
		}
		
		# build qSeqs if needed
		if(@QStrs == 0){
			for(my $i=0;$i<@y;$i++){
				$QStrs[$i] = substr($s[9],$y[$i],$x[$i]);
			}
		}
	}

	# CIGAR computation
	$cigar .= $t[11].$clipChar if ($t[11]); # 5'-end clipping
	my ($x0, $y0, $z0) = ($x[0], $y[0], $z[0]);  # Retrieve first (blockSizes, qStarts, tStarts) set.
	for my $i (1 .. $t[17]-1) {   #process block by block, start from 2nd block,  $t[17]-> Number of blocks in the alignment
		my $ly = $y[$i] - $y[$i-1] - $x0;  # $ly -> gap between query blocks  (Insertion)
		my $lz = $z[$i] - $z[$i-1] - $x0;  # $lz -> gap between target blocks (Deletion)
		
		$cigar .= ($x0) . 'M';
		$cigar .= $ly . 'I'if $ly!=0;
		$cigar .=  $lz . 'D'if $lz!=0;
		$NM += $ly+$lz;
		($x0, $y0, $z0) = ($x[$i], $y[$i], $z[$i]);
	}
	$cigar .= ($x0) . 'M';
	$cigar .= ($t[10] - $t[12]).$clipChar if ($t[10] != $t[12]); # 3'-end clipping

	# MD computation, if required	
	if ($MD) {
		my $MDcount = 0;
		my $Deletion=0;
		
		die("Error: MD tag requires PSLX format file.\n") if @QStrs == 0;

		if ( @TStrs > 0 ){
			# do nothing
		}else{
			for my $i (0 .. $t[17]-1) {
				$TStrs[$i] = uc(substr($seqdb{$t[13]},$z[$i],$x[$i]));
			}
		}
		
		($x0, $y0, $z0) = ($x[0], $y[0], $z[0]);  # Retrieve first (blockSizes, qStarts, tStarts) set.
		for my $i (0 .. $t[17]-1) {   #process block by block, start from 2nd block,  $t[17]-> Number of blocks in the alignment
			if($i>0){
				my $ly = $y[$i] - $y[$i-1] - $x0;  # $ly -> gap between query blocks  (Insertion)
				my $lz = $z[$i] - $z[$i-1] - $x0;  # $lz -> gap between target blocks (Deletion)
				if ($lz>0) {  #Deletion
					$MDstr .= $MDcount if $MDcount>0;
					$MDcount=0;
					my $Dstart = $z[$i-1]+$x0;
					$MDstr .= "^".uc(substr($seqdb{$t[13]},$Dstart,$lz));
					$Deletion=1;
				}
				($x0, $y0, $z0) = ($x[$i], $y[$i], $z[$i]);
			}
			
			my @Qstr = split("",$QStrs[$i]);
			my @Tstr = split("",$TStrs[$i]);
			for my $j (0..@Qstr-1) {
				$MDcount++ if $Qstr[$j] eq $Tstr[$j];
				if ($Qstr[$j] ne $Tstr[$j]) {
					if ($MDcount) {
						$MDstr .= $MDcount.uc($Tstr[$j]);
						$MDcount = 0;
					}else{
						$MDstr .= 0 if $Deletion&&$j==0;
						$MDstr .= uc $Tstr[$j];
					}
				}
			}
			$Deletion=0;
		}
		$MDstr .= $MDcount if $MDcount;
	}
	
	$s[5] = $cigar;
	my $XM=$t[1]+$t[3];
	$NM += $XM;
	$s[11] = "NM:i:$NM";
	$s[12] = "XM:i:$XM";
	$s[13] = "MD:Z:$MDstr" if $MD;
	print join("\t", @s), "\n";
}
close pslDATA;

sub removeMinor {
	my $xRef = shift;
	my $yRef = shift;
	my $zRef = shift;
	my $qStrsRef = shift;
	my $tStrsRef = shift;
	my $minorLimit = shift;
	
	my $beenModified = 0;
	
	# collect minor blocks
	my %sizeIdxHash;
	for(my $idx=0;$idx<@{$xRef};$idx++){
		push @{$sizeIdxHash{$$xRef[$idx]}},$idx if( $$xRef[$idx]<=$minorLimit );
	}
	
	# from smaller & latter blocks
	while((keys %sizeIdxHash)>0){
		my $size = (sort {$a<=>$b} keys %sizeIdxHash)[0]; # smallest size
		for my $idx (sort {$b<=>$a} @{$sizeIdxHash{$size}}){ # from latter block
			next if @{$xRef}<=1; # not to remove when number of blocks <=1
			$beenModified = 1;
		
			splice @{$xRef},$idx,1;
			splice @{$yRef},$idx,1;
			splice @{$zRef},$idx,1;
			splice @{$qStrsRef},$idx,1 if @{$qStrsRef}>0;
			splice @{$tStrsRef},$idx,1 if @{$tStrsRef}>0;
		}
		
		# refresh %sizeIdxHash
		%sizeIdxHash=();
		for(my $idx=0;$idx<@{$xRef};$idx++){
			push @{$sizeIdxHash{$$xRef[$idx]}},$idx if( $$xRef[$idx]<=$minorLimit );
		}
	}
	
	return $beenModified;
}

# return the index i indicating that the i-th block is overlapping with (i-1)st block, 0 for no overlapping
sub overlapCheck {
	my $xRef = shift;
	my $yRef = shift;
	my $zRef = shift;
	
	my $overlapIdx = 0;
	
	for(my $idx=1;$idx<@{$xRef};$idx++){
		if( ($$yRef[$idx] - $$yRef[$idx-1] - $$xRef[$idx-1])<0 || ($$zRef[$idx] - $$zRef[$idx-1] - $$xRef[$idx-1])<0 ){
			$overlapIdx = $idx;
			last;
		}
	}
	
	return $overlapIdx;
}

sub overlapFix {
	my $xRef = shift;
	my $yRef = shift;
	my $zRef = shift;
	my $overlapIdx = shift;
	my $qStrsRef = shift;
	my $tStrsRef = shift;

	# fix block at overlapIdx, if it is the smaller one
	if($$xRef[$overlapIdx]<$$xRef[$overlapIdx-1]){
		my $ly = $$yRef[$overlapIdx] - $$yRef[$overlapIdx-1] - $$xRef[$overlapIdx-1];  # $ly -> gap between query blocks  (Insertion)
		my $lz = $$zRef[$overlapIdx] - $$zRef[$overlapIdx-1] - $$xRef[$overlapIdx-1];  # $lz -> gap between target blocks (Deletion)
		
		# adjustment for avoiding negative I's and D's
		if($ly<0){
			$$xRef[$overlapIdx] += $ly;
			$$yRef[$overlapIdx] -= $ly;
			$$zRef[$overlapIdx] -= $ly;
			$$qStrsRef[$overlapIdx] = substr($$qStrsRef[$overlapIdx],-$ly) if @{$qStrsRef}>0 && $$xRef[$overlapIdx]>0;
			$$tStrsRef[$overlapIdx] = substr($$tStrsRef[$overlapIdx],-$ly) if @{$tStrsRef}>0 && $$xRef[$overlapIdx]>0;
		}
		elsif($lz<0){
			$$xRef[$overlapIdx] += $lz;
			$$yRef[$overlapIdx] -= $lz;
			$$zRef[$overlapIdx] -= $lz;
			$$qStrsRef[$overlapIdx] = substr($$qStrsRef[$overlapIdx],-$lz) if @{$qStrsRef}>0 && $$xRef[$overlapIdx-1]>0;
			$$tStrsRef[$overlapIdx] = substr($$tStrsRef[$overlapIdx],-$lz) if @{$tStrsRef}>0 && $$xRef[$overlapIdx-1]>0;
		}

		# remove the block if negative after adjustment
		if($$xRef[$overlapIdx]<=0){
			splice @{$xRef},$overlapIdx,1;
			splice @{$yRef},$overlapIdx,1;
			splice @{$zRef},$overlapIdx,1;
			splice @{$qStrsRef},$overlapIdx,1 if @{$qStrsRef}>0;
			splice @{$tStrsRef},$overlapIdx,1 if @{$tStrsRef}>0;
		}
	}
	# fix block at overlapIdx-1
	else{
		my $ly = $$yRef[$overlapIdx] - $$yRef[$overlapIdx-1] - $$xRef[$overlapIdx-1];  # $ly -> gap between query blocks  (Insertion)
		my $lz = $$zRef[$overlapIdx] - $$zRef[$overlapIdx-1] - $$xRef[$overlapIdx-1];  # $lz -> gap between target blocks (Deletion)
		
		# adjustment for avoiding negative I's and D's
		if($ly<0){
			$$xRef[$overlapIdx-1] += $ly;
			$$qStrsRef[$overlapIdx-1] = substr($$qStrsRef[$overlapIdx-1],0,$ly) if @{$qStrsRef}>0 && $$xRef[$overlapIdx-1]>0;
			$$tStrsRef[$overlapIdx-1] = substr($$tStrsRef[$overlapIdx-1],0,$ly) if @{$tStrsRef}>0 && $$xRef[$overlapIdx-1]>0;
		}
		elsif($lz<0){
			$$xRef[$overlapIdx-1] += $lz;
			$$qStrsRef[$overlapIdx-1] = substr($$qStrsRef[$overlapIdx-1],0,$lz) if @{$qStrsRef}>0 && $$xRef[$overlapIdx-1]>0;
			$$tStrsRef[$overlapIdx-1] = substr($$tStrsRef[$overlapIdx-1],0,$lz) if @{$tStrsRef}>0 && $$xRef[$overlapIdx-1]>0;
		}

		# remove the block if negative after adjustment
		if($$xRef[$overlapIdx-1]<=0){
			splice @{$xRef},$overlapIdx-1,1;
			splice @{$yRef},$overlapIdx-1,1;
			splice @{$zRef},$overlapIdx-1,1;
			splice @{$qStrsRef},$overlapIdx-1,1 if @{$qStrsRef}>0;
			splice @{$tStrsRef},$overlapIdx-1,1 if @{$tStrsRef}>0;
		}
	}
}
