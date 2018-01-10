#!/usr/bin/perl -w
use Bio::DB::Fasta;

my $usage = "Usage: PTCdetection.pl <strandCGFF> <cdsCGFF> <modelGeneMap> <eventList> <genomeFasta> <outFilename>\n";
my $strandCGFF = shift or die $usage;
my $cdsCGFF = shift or die $usage;
my $geneModelFile = shift or die $usage;
my $eventListFile = shift or die $usage;
my $genomeFasta = shift or die $usage;
my $outFilename = shift or die $usage;


my $genomeDB = Bio::DB::Fasta->new($genomeFasta);

# gene info & gene exon intervals
my %geneInfoHOA = ();
my %cgffHOA = ();
open(FILE,"<$strandCGFF");
my $geneID;
while( $line = <FILE> ) {
	my @token = split(/\s+/, $line);
	if( $line=~/^>/ ) {
		$geneID = substr($token[0],1);
		if(@token>4) {
			# chr in array[0] and strand in array[1]
			push @{ $geneInfoHOA{$geneID} },trim($token[1]),trim($token[4]); 
		}else{
			die "$strandCGFF not a strand CGFF\n"
		}
	}else{
		push @{$cgffHOA{$geneID}},trim($token[0]),trim($token[1]); 
	}
}
close FILE;

# cds block intervals
my %cdsCgffHOA = ();
open(FILE,"<$cdsCGFF");
my $mRNAID;
while( $line = <FILE> ) {
	my @token = split(/\s+/, $line);
	if( $line=~/^>/ ) {
		$mRNAID = substr($token[0],1);
	}else{
		push @{$cdsCgffHOA{$mRNAID}},trim($token[0]),trim($token[1]); 
	}
}
close FILE;

# for seq to AA translation
my %genetic_code = getTranslateMap();

# gene to model mapping
my %geneModelMap = ();
open(FILE,"<$geneModelFile");
while(<FILE>) { # token[1]: gene, token[0]: mRNA
	my @token = split(/\s+/,trim($_));
	push(@{$geneModelMap{$token[1]}},$token[0]);
}
close FILE;

# for case insensitive seq names
my @seqIds = $genomeDB->ids;
my %seqNameMap = ();
for(my $idx = 0; $idx < @seqIds; $idx++) {
	$seqNameMap{uc($seqIds[$idx])} = $seqIds[$idx];
}
#reorganize geneID & event
open(FILE,"<$eventListFile");
my %genePatternHash=();
while(<FILE>){
	chomp;
	my @token=split;
	next unless @token > 0;
	$genePatternHash{$token[0]}{$token[1]}=0;
}
close FILE;

open(OUT,">$outFilename");
print OUT "GeneID\tpattern\tTransctipt\taffectedSize\tmodulo3\tPTC\tirPTC\tSEQ\tirSEQ\texistingModels\n";
#print OUT "0: GeneID, 1: pattern, 2*: Transctipt, 3*: affectedSize, 4*: modulo3, 5*: PTC, 6*: irPTC, 7: SEQ, 8*: irSEQ, 9:existingModels";


# Main processing
for my $geneID (sort keys %genePatternHash){
	# construct a proteinSeqModelHOA
	my %proteinSeqModelHOA;
	my @geneModels;
	@geneModels = @{$geneModelMap{$geneID}} if exists $geneModelMap{$geneID};
	my ($geneChr,$geneStrand)=($geneInfoHOA{$geneID}[0],$geneInfoHOA{$geneID}[1]);
	
	for $model (@geneModels) {
		my $proteinSeq = seq2aa(getModelSeq($model,$geneChr,$geneStrand,\%cdsCgffHOA,$genomeDB));
		push(@{$proteinSeqModelHOA{$proteinSeq}},$model);
	}
	
	for my $pattern (sort keys %{$genePatternHash{$geneID}}) {
		my %resultHash = ();

		my @ResultAr;
		if ($pattern=~/^\d+$/) { # IR
			@ResultAr = irEvent("$geneID\t$pattern",\%geneInfoHOA,\%seqNameMap,\%cgffHOA,\%geneModelMap,\%cdsCgffHOA,$genomeDB);
		}else{ # other cases
			# retrieve event splice info
			my ($spGeneID,$spliceStart,$spliceEnd)=splicePos("$geneID\t$pattern",\%cgffHOA);
			next if $spGeneID eq "null" && $spliceStart == -1;
			
			# get event PTC
			@ResultAr = splicePTC($geneID,$pattern,$spliceStart,$spliceEnd,\%geneInfoHOA,\%seqNameMap,\%geneModelMap,\%cdsCgffHOA,$genomeDB);
		}
		
		# result merge
		for (@ResultAr) {
			my @Results=@{$_};
			my $seq = $Results[7];
			push @{$resultHash{$seq}}, \@Results;
		}
		
		# report combine
		for my $key (keys %resultHash) {
			my @reportToken=@{$resultHash{$key}[0]};

			# processing tokens 0~7
			for(my $idx=1;$idx<@{$resultHash{$key}}; $idx++){
				my @thisReport=@{$resultHash{$key}[$idx]};
				$reportToken[2].="; $thisReport[2]";
				$reportToken[3].="; $thisReport[3]";
				$reportToken[4].="; $thisReport[4]";
				$reportToken[5].="; $thisReport[5]";
				$reportToken[6].="; $thisReport[6]";
			}
			# processing token 8 (irSEQ)
			my %irSeqHash = ();
			for(my $idx=0;$idx<@{$resultHash{$key}}; $idx++){
				my @thisReport=@{$resultHash{$key}[$idx]};
				my $model = $thisReport[2];
				my $irSEQ = $thisReport[8];
				push @{$irSeqHash{$irSEQ}}, $model;
			}
			my @irSeqs = (keys %irSeqHash);
			if( @irSeqs==1 ){
				$reportToken[8] = $irSeqs[0];
			}else{
				$reportToken[8]="";
				for my $irSEQ (sort { $irSeqHash{$a}[0] cmp $irSeqHash{$b}[0] } keys %irSeqHash){
					$reportToken[8] .= join(",",sort @{$irSeqHash{$irSEQ}});
					$reportToken[8] .= "=";
					$reportToken[8] .= $irSEQ;
					$reportToken[8] .= "; "
				}
			}
			# check if SEQ (token 7) the same with some model (proteinSeqModelHOA)
			# report existing model
			my $aaSeq = $reportToken[7];
			if (!exists $proteinSeqModelHOA{$aaSeq}) {
				print OUT join("\t",@reportToken)."\n";
			}else{
				my $existModel = join(",", sort @{$proteinSeqModelHOA{$aaSeq}});
				print OUT join("\t",@reportToken)."\t$existModel\n";
			}
		}
	}
}
close OUT;


## subroutines
sub trim {
	my $str=shift;
	$str =~ s/(^\s+|\s+$)//g;
	return $str;
}

sub getTranslateMap{
	my (%retuenMap) = (
		'TCA' => 'S',    #Serine
		'TCC' => 'S',    #Serine
		'TCG' => 'S',    #Serine
		'TCT' => 'S',    #Serine
		'TCN' => 'S',    #Serine
		'TTC' => 'F',    #Phenylalanine
		'TTT' => 'F',    #Phenylalanine
		'TTA' => 'L',    #Leucine
		'TTG' => 'L',    #Leucine
		'TAC' => 'Y',    #Tyrosine
		'TAT' => 'Y',    #Tyrosine
		'TAA' => '*',    #Stop
		'TAG' => '*',    #Stop
		'TGC' => 'C',    #Cysteine
		'TGT' => 'C',    #Cysteine
		'TGA' => '*',    #Stop
		'TGG' => 'W',    #Tryptophan
		'CTA' => 'L',    #Leucine
		'CTC' => 'L',    #Leucine
		'CTG' => 'L',    #Leucine
		'CTT' => 'L',    #Leucine
		'CTN' => 'L',    #Leucine
		'CAT' => 'H',    #Histidine
		'CAC' => 'H',    #Histidine
		'CAA' => 'Q',    #Glutamine
		'CAG' => 'Q',    #Glutamine
		'CGA' => 'R',    #Arginine
		'CGC' => 'R',    #Arginine
		'CGG' => 'R',    #Arginine
		'CGT' => 'R',    #Arginine
		'CGN' => 'R',    #Arginine
		'ATA' => 'I',    #Isoleucine
		'ATC' => 'I',    #Isoleucine
		'ATT' => 'I',    #Isoleucine
		'ATG' => 'M',    #Methionine
		'ACA' => 'T',    #Threonine
		'ACC' => 'T',    #Threonine
		'ACG' => 'T',    #Threonine
		'ACT' => 'T',    #Threonine
		'ACN' => 'T',    #Threonine
		'AAC' => 'N',    #Asparagine
		'AAT' => 'N',    #Asparagine
		'AAA' => 'K',    #Lysine
		'AAG' => 'K',    #Lysine
		'AGC' => 'S',    #Serine
		'AGT' => 'S',    #Serine
		'AGA' => 'R',    #Arginine
		'AGG' => 'R',    #Arginine
		'CCA' => 'P',    #Proline
		'CCC' => 'P',    #Proline
		'CCG' => 'P',    #Proline
		'CCT' => 'P',    #Proline
		'CCN' => 'P',    #Proline
		'GTA' => 'V',    #Valine
		'GTC' => 'V',    #Valine
		'GTG' => 'V',    #Valine
		'GTT' => 'V',    #Valine
		'GTN' => 'V',    #Valine
		'GCA' => 'A',    #Alanine
		'GCC' => 'A',    #Alanine
		'GCG' => 'A',    #Alanine
		'GCT' => 'A',    #Alanine
		'GCN' => 'A',    #Alanine
		'GAC' => 'D',    #Aspartic Acid
		'GAT' => 'D',    #Aspartic Acid
		'GAA' => 'E',    #Glutamic Acid
		'GAG' => 'E',    #Glutamic Acid
		'GGA' => 'G',    #Glycine
		'GGC' => 'G',    #Glycine
		'GGG' => 'G',    #Glycine
		'GGT' => 'G',    #Glycine
		'GGN' => 'G',    #Glycine
	);
	return %retuenMap;
}

sub seq2aa{
	my $seq=uc $_[0];
	my $Ns = 3-(length($seq)%3);
	$seq .= 'N' x $Ns if $Ns < 3;  # complement Sequence to 3mer
	
	my $AA_seq='';
	for $k (0 .. length($seq)/3-1) {
		my $codon = substr($seq,3*$k,3);
		
		if(exists $genetic_code{$codon}) {
			$AA_seq.=$genetic_code{$codon};
		}else{
			$AA_seq.="x";
		}
	}
	return $AA_seq;
}

#splicePos($eventPattern,\%cgffHOA);
sub splicePos {
	my ($geneID,$start,$end)=("null",-1,-1);
	if ($_[0] =~ /(\S+)\t(\d+)\[(\d+)-(\d+)\]/) { #exonX[relativePosA-relativePosB]
		my $exon1 = $2;
		my $exon2 = $2;
		$geneID = $1;
		$start = $_[1]{$geneID}[2*($exon1-1)]+$3;
		$end = $_[1]{$geneID}[2*($exon1-1)]+$4;
	}elsif ($_[0] =~ /(\S+)\t(\d+)\(([+|-]?\d+)\)-(\d+)\(([+|-]?\d+)\)/) {  #exonA(relativePosA)-exonB(relativePosB)
		my $exon1 = $2;
		my $exon2 = $4;
		$geneID = $1;
		$start = $_[1]{$geneID}[2*($exon1-1)+1]+$3;
		$end = $_[1]{$geneID}[2*($exon2-1)]-$5;
	}elsif ($_[0] =~ /(\S+)\t(\d+)-(\d+)\(([+|-]?\d+)\)/) { # genomicPosA-exonB(relativePosB)
		my $exon2 = $3;
		$geneID = $1;
		$start = $2;
		$end = $_[1]{$geneID}[2*($exon2-1)]-$4;
	}elsif ($_[0] =~ /(\S+)\t(\d+)\(([+|-]?\d+)\)-(\d+)/) { # exonA(relativePosA)-genomicPosB
		my $exon1 = $2;
		$geneID = $1;
		$start = $_[1]{$geneID}[2*($exon1-1)+1]+$3;
		$end = $4;
	}elsif ($_[0] =~ /(\S+)\t(\d+)-(\d+)/) { #genomicPosA-genomicPosB
		$geneID = $1;
		$start = $2;
		$end = $3;
	}elsif ($_[0] =~ /(\S+)\t(\d+)\<=\>(\d+)/) { #exonA<=>exonB
		my $exon1 = $2;
		my $exon2 = $3;
		$geneID = $1;
		$start = $_[1]{$geneID}[2*($exon1-1)+1];
		$end = $_[1]{$geneID}[2*($exon2-1)];
	}
	return ($geneID,$start,$end);
}

#splicePTC($geneID,$event,$spliceStart,$spliceEnd,\%geneInfoHOA,\%seqNameMap,\%geneModelMap,\%cdsCgffHOA,$genomeDB);
sub splicePTC {
	my $gene=$_[0];
	my $event=$_[1];
	my $gap_start=$_[2]+1;
	my $gap_end=$_[3]-1;
	my $strand = $_[4]{$gene}[1];  # %geneInfoHOA
	my $chr = $_[5]{uc($_[4]{$gene}[0])}; # %seqNameMap
	
	my $intronSize = $gap_end-$gap_start+1;
	
	my @AnsAr;
	my @models = ();
	if(exists $_[6]{$gene}){  # %geneModelMap
		@models = @{$_[6]{$gene}};
	}else{
		my @Ans = ($gene,$event,"no mRNA ID","-","-","-","-","-","-");
		push(@AnsAr,\@Ans);
		return @AnsAr
	}
	# cdsCGFF
	for $mRNAID (@models) {
		my @cdsIntervalAr = (); # %cdsCgffHOA
		@cdsIntervalAr = @{$_[7]{$mRNAID}} if exists $_[7]{$mRNAID}; # %cdsCgffHOA , get cds blocks interval
		my @modelIntronExonAoA=getIntronExonAoA(@cdsIntervalAr); # get cds/intron interval array of array
		
		my ($cdsSeq,$ptcSeq)=("","");
		my $affectedSize=0;
		for $block (@modelIntronExonAoA) {
			my $mode=$$block[0];
			my $start=$$block[1];
			my $end=$$block[2];
			my @tbtBlocks=intersectSubstract($start,$end,$gap_start,$gap_end,$mode);
			
			#cds
			my @cdsBlock=([$start,$end]);
			$cdsSeq .= getSeq(\@cdsBlock,$chr,$_[8]) if $mode eq "cds"; # $_[8] = $genomeDB
			#PTC
			$ptcSeq .= getSeq(\@tbtBlocks,$chr,$_[8]);
			
			# compute affected size
			if($mode eq "cds"){
				$affectedSize += getTotalLength(\@cdsBlock)-getTotalLength(\@tbtBlocks);
			}elsif($mode eq "intron"){
				$affectedSize += getTotalLength(\@tbtBlocks);
			}
		}
		next if $cdsSeq eq $ptcSeq;
		my $ptcLengthDiff=length($ptcSeq)-length($cdsSeq);
		
		if ($strand eq "-") {
			$ptcSeq = reverse $ptcSeq;
			$ptcSeq =~ tr/ATCG/TAGC/;
		}
		my $ptc_AAseq = seq2aa($ptcSeq);
		my $PTC=0;
		if ($ptc_AAseq =~ /\*$/) {
			my $cnt = $ptc_AAseq =~ tr/*/*/;
			$PTC = 1 if $cnt >1;
		}else{
			$PTC = 1 if $ptc_AAseq =~ /\*/;
		}
		my @Ans=($gene,$event,$mRNAID,"$affectedSize($ptcLengthDiff)",$ptcLengthDiff%3,$PTC,"-",$ptc_AAseq,"-");
		push(@AnsAr,\@Ans);
	}
	return @AnsAr;
}

#intersectSubstract($start,$end,$splice_start,$splice_end,$mode)
sub intersectSubstract {
	my ($blockStart,$blockEnd)=($_[0],$_[1]);
	my ($gapStart,$gapEnd)=($_[2],$_[3]);
	my @intersectAr;
	if ( intersect($blockStart,$blockEnd,$gapStart,$gapEnd) ) {
		if ($gapStart <= $blockStart) {
			#case1, give 1 block  | [ | ]
			push(@intersectAr,[$gapEnd+1,$blockEnd]) if $gapEnd<$blockEnd ; 
		}elsif ($gapStart > $blockStart) {
			if ($gapEnd < $blockEnd) {
				# case2, give 2 blocks [ || ]
				push(@intersectAr,[$blockStart,$gapStart-1]);
				push(@intersectAr,[$gapEnd+1,$blockEnd]);
			}elsif ($gapEnd >= $blockEnd) {
				# case3 give 1 block [ | ] |
				push(@intersectAr,[$blockStart,$gapStart-1]);
			}
		}
	}else{ # CDS no intersect return 
		push(@intersectAr,[$blockStart,$blockEnd]) if $_[4] eq "cds";
	}
	return @intersectAr;
}

sub intersect {
	my ($blockStart,$blockEnd)=($_[0],$_[1]);
	my ($gapStart,$gapEnd)=($_[2],$_[3]);
	if ( ($blockStart <= $gapStart && $gapStart <= $blockEnd) || ($blockStart <= $gapEnd && $gapEnd <= $blockEnd)
          || ($gapStart <= $blockStart && $blockEnd <= $gapEnd)) {
		return 1;
	}else{
		return 0;
	}
}

#getIntronExonAoA(@cdsIntervalAr);
sub getIntronExonAoA {
	my $cdsPair_num=@_/2;
	my $cdsIntronPair=$cdsPair_num+$cdsPair_num-1;
	my @returnAoA;
	for (my $i=0; $i < $cdsIntronPair; $i++) {
		my $mode="cds";
		$mode="intron" if $i%2;
		if ($mode eq "cds") {
			my $start=$_[$i];
			my $end=$_[$i+1];
			push(@returnAoA,[$mode,$start,$end]);
		}elsif ($mode eq "intron") {
			my $start=$_[$i]+1;
			my $end=$_[$i+1]-1;
			push(@returnAoA,[$mode,$start,$end]);
		}
	}
	return @returnAoA;
}

#getSeq(\@blocks,$chr,$genomeDB);
sub getSeq {
	my $ansSeq="";
	for $intervalAr (@{$_[0]}) {
		$ansSeq .= $_[2]->seq($_[1],$$intervalAr[0],$$intervalAr[1]); # $genomeDB->seq($chr,$start,$end)
	}
	return uc($ansSeq)
}

#getTotalLength(\@blocks);
sub getTotalLength {
	my $ans=0;
	for $intervalAr (@{$_[0]}) {
		$ans += ($$intervalAr[1]-$$intervalAr[0]+1);
	}
	return $ans;
}

#getModelSeq($model,$geneChr,$geneStrand,\%cdsCgffHOA,$genomeDB)
sub getModelSeq {
	my $seq="";
	my @cdsCgffAr = ();
	@cdsCgffAr = @{$_[3]{$_[0]}} if exists $_[3]{$_[0]}; # %cdsCgffHOA
	
	for (my $idx=0; $idx < @cdsCgffAr/2 ; $idx++) {
		my ($start,$end) = ($cdsCgffAr[2*$idx],$cdsCgffAr[2*$idx+1]);
		$seq .= $_[4]->seq($_[1],$start,$end); # %genomeDB
	}
	
	if ($_[2] eq "-") {
		$seq = reverse(uc($seq));
		$seq =~ tr/ATCG/TAGC/;
	}
	return $seq;
}

sub irEvent {
	# strandCGFF
	my @is = split('\s',$_[0]);
	my $gene = $is[0];
	my $irNum = $is[1];
	my $strand = $_[1]{$gene}[1];  # %geneInfoHOA
	my $chr = $_[2]{uc($_[1]{$gene}[0])}; # %seqNameMap
	my @cgffAr = @{$_[3]{$gene}}; # %cgffHOA
	my $allExonPair = @cgffAr/2;

	# calculate intron retention start & end position
	my $irExonPair = $irNum-1;
	my $startExon_IDX=2*$irExonPair+1;
	my $endExon_IDX = 2*$irExonPair+2;
	my $eventStart=$cgffAr[$startExon_IDX]+1;
	my $eventEnd=$cgffAr[$endExon_IDX]-1;
	
	my @AnsAr;
	
	my @mRNA = ();
	if(exists $_[4]{$gene}){  # %geneModelMap
		@mRNA = @{$_[4]{$gene}};
	}else{
		my @Ans = ($gene,$irNum,"no mRNA ID","-","-","-","-","-","-");
		push(@AnsAr,\@Ans);
		return @AnsAr
	}
	# cdsCGFF
	for $mRNAID (@mRNA) {
		my @cdsIntervalAr = (); # %cdsCgffHOA
		@cdsIntervalAr = @{$_[5]{$mRNAID}} if exists $_[5]{$mRNAID}; # %cdsCgffHOA , get cds blocks interval
		next if @cdsIntervalAr <= 2; #skip model if contains NO intron
		my @modelIntronExonAoA=getIntronExonAoA(@cdsIntervalAr); # get cds/intron interval array of array
		
		# skip this CDS if IR not intersects this CDS
		my ($chk_cdsStart,$chk_cdsEnd) = ($cdsIntervalAr[0],$cdsIntervalAr[@cdsIntervalAr-1]);
		next if not intersect($chk_cdsStart,$chk_cdsEnd,$eventStart,$eventEnd);
		
		my @totalSeqBlockAoA;
		my $affectedSize=0;
		my ($retentGenomeStart,$retentGenomeEnd);
		my @eventLeftRightCDSLength;
		my $eIDX = 0;
		
		for $block (@modelIntronExonAoA) {
			my $mode=$$block[0];
			my $start=$$block[1];
			my $end=$$block[2];
			
			if ($mode eq "cds") {
				push(@totalSeqBlockAoA,[$start,$end]);
				$eventLeftRightCDSLength[$eIDX] += ($end-$start+1);
			}elsif ($mode eq "intron" && intersect($eventStart,$eventEnd,$start,$end) ) {
				push(@totalSeqBlockAoA,[$start,$end]);
				$retentGenomeStart = $start unless defined $retentGenomeStart;
				$retentGenomeEnd = $end;
				$affectedSize += ($end-$start+1);
				$eIDX++;
			}
		}
		# skip this CDS if retentGenomeStart undefined, which means the IR region intersects no introns of the model
		next if not defined $retentGenomeStart;
		
		my ($eventLeftCDS,$eventRightCDS) = ($eventLeftRightCDSLength[0],$eventLeftRightCDSLength[@eventLeftRightCDSLength-1]);
		my @irStartStopAr = irRegionCompletment($eventLeftCDS,$eventRightCDS,$retentGenomeStart,$retentGenomeEnd,$strand);
		
		my $irSeq = getSeq(\@irStartStopAr,$chr,$_[6]);
		my $totalSeq = getSeq(\@totalSeqBlockAoA,$chr,$_[6]);
		
		if ($strand eq "-") {
			$totalSeq = reverse $totalSeq;
			$totalSeq =~ tr/ATCG/TAGC/;
			$irSeq = reverse $irSeq;
			$irSeq =~ tr/ATCG/TAGC/;
		}
		
		my $total_AAseq = seq2aa($totalSeq);
		my $ir_AAseq = seq2aa($irSeq);
		my ($PTC,$irPTC)=(0,0);
		if ($total_AAseq =~ /\*$/) { # PTC if any stop codon not at the end of the AA sequence
			my $cnt = $total_AAseq =~ tr/*/*/;
			$PTC = 1 if $cnt >1;
		}else{
			$PTC = 1 if $total_AAseq =~ /\*/;
		}
		$irPTC = 1 if $ir_AAseq =~ /\*/;
		my @Ans=($gene,$irNum,$mRNAID,$affectedSize,$affectedSize%3,$PTC,$irPTC,$total_AAseq,$ir_AAseq);
		push(@AnsAr,\@Ans);
	}
	return @AnsAr;
}

#irRegionCompletment($eventLeftCDS,$eventRightCDS,$retentGenomeStart,$retentGenomeEnd,$strand);
sub irRegionCompletment {
	my ($leftCDSLength,$rightCDSLength)=($_[0],$_[1]);
	my ($irStart,$irEnd)=($_[2],$_[3]);
	my $geneStrand = $_[4];
	
	my @returnAr;
	my ($complement_irStart,$complement_irEnd);
	if ($geneStrand eq "+") {
		my $left_shift = $leftCDSLength%3;
		
		my $right_shift = ($leftCDSLength + ($irEnd - $irStart +1))%3;
		$right_shift = 3 - $right_shift if $right_shift != 0;
		
		$complement_irStart = $irStart - $left_shift;
		$complement_irEnd = $irEnd + $right_shift;
	}elsif ($geneStrand eq "-") {
		my $right_shift = $rightCDSLength%3;
		
		my $left_shift = ($rightCDSLength + ($irEnd - $irStart +1))%3;
		$left_shift = 3 - $left_shift if $left_shift != 0;
		
		$complement_irStart = $irStart - $left_shift;
		$complement_irEnd = $irEnd + $right_shift;
	}
	
	push(@returnAr,[$complement_irStart,$complement_irEnd]);
	return @returnAr;
}