#!/usr/bin/perl -w
use warnings;
use Bio::DB::Fasta;
use IPC::Open2;
use Data::Dumper;
$|=1;

my $debug=0;
my $readfilter=-1;
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-minread') {
		$readfilter=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-debug') {
		$debug=1;
		delete $arg_idx[$i];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if defined $_; }
@ARGV=@new_arg;

my $usage  = "Usage: MutationAnnotation.pl <SNP record file> <rackj JAR path> <CDS CGFF> <UTR CGFF> <exon CGFF> <genome FASTA> [-minread <conut>] [-debug]\n";
my $SNPrec = shift or die $usage;
my $jarPath = shift or die $usage;
my $cdsCGFF = shift or die $usage;
my $utrCGFF = shift or die $usage;
my $exonCGFF = shift or die $usage;
my $FastaDB = shift or die $usage;

# query gene region by -exon false
my $pid = open2( *exonReport, *exonCheck,"java -classpath $jarPath special.GeneQuery -exon false -GFF $exonCGFF");
my $pid2 = open2( *cdsReport, *cdsCheck,"java -classpath $jarPath special.GeneQuery -exon true -GFF $cdsCGFF");
my $pid3 = open2( *utrReport, *utrCheck,"java -classpath $jarPath special.GeneQuery -exon true -GFF $utrCGFF");

my $genomeDB = Bio::DB::Fasta->new($FastaDB);
# for case insensitive seq names
my @seqIds = $genomeDB->ids;
my %seqNameMap;
for (my $idx = 0; $idx < @seqIds; $idx++) {
    $seqNameMap{lc($seqIds[$idx])} = $seqIds[$idx];
}

my %cdsMap = getIntervals($cdsCGFF);
my %exonMap = getIntervals($exonCGFF);
my %intronMap = getIntrons(\%exonMap);

my (%genetic_code) = (
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

print "#type\tchr\tstart\tlocation\tmutation\t#reads\tcoverage\tratio\tannotation\tgene\teffect\n";
my %codeMap;
open( LISTFILE, "<$SNPrec" );
while (my $line=<LISTFILE>) {
	my @t = split(/\s+/,$line);
	my @SNP; # snp nucleotide
	my @INDEL; # indel gap
	my $reads=0;
	my $coverage=0;
	if ($line =~ /.+\(\d+,\d+\):(\d+):(\d+)\s\S*\s(\d+)\s(\d+)\s.+/) { #  INDEL pattern,from NtFreqCounter
		@INDEL=($1,$2);
		$coverage=$3;
		$reads = $4;
	} elsif ($line =~ /.+:\d+:(.)->(.)\s\S*\s(\d+)\s(\d+)\s(\d+)\s(\d+)\s(\d+)\s(\d+)/) { # from NtFreqCounter
		@SNP = ($1,$2);
		$coverage=$3;
		$reads = eval "\$$codeMap{$SNP[1]}";
		$line = $t[0];
	} elsif ($t[0] =~ /.+:\d+:(.)->(.)/) { # current SNP notation
		@SNP = ($1,$2);
		$reads = $t[1];
	} elsif ($line =~ /#\w+\sHitGenes\sReads\sFreq\((.)\)\sFreq\((.)\)\sFreq\((.)\)\sFreq\((.)\)\sFreq\((.)\)/) { # from NtFreqCounter, first line
		for $i (1..5) {
			eval "\$codeMap{\$$i}=$i+3";
		}
	}
	next if $line =~ /^#/;
	next if @SNP && $SNP[1] eq 'N';
	next if $reads < $readfilter;
	
	# $t[0] adjustment for special.GeneQuery
	if($t[0]=~/(.+\(\d+,\d+\)):\d+:\d+/){
		$t[0] = $1;
	}elsif($t[0]=~/(.+:\d+):.->./){
		$t[0] = $1;
	}
	
	my $geneMsg;
	my $cdsMsg;
	my $utrMsg;
	my $exonMsg;
	if(@INDEL){
		print exonCheck "$t[0]\tfalse\tfalse\n"; 
		$geneMsg = <exonReport>;
		print cdsCheck "$t[0]\ttrue\tfalse\n";
		$cdsMsg = <cdsReport>;
		print utrCheck "$t[0]\ttrue\tfalse\n";
		$utrMsg  = <utrReport>;
		print exonCheck "$t[0]\ttrue\tfalse\n";
		$exonMsg = <exonReport>;
	}else{
 		print exonCheck "$t[0]\n";
		$geneMsg = <exonReport>;
		print cdsCheck "$t[0]\n";
		$cdsMsg = <cdsReport>;
		print utrCheck "$t[0]\n";
		$utrMsg  = <utrReport>;
		print exonCheck "$t[0]\ttrue\n";
		$exonMsg = <exonReport>;
	}
	chomp($geneMsg);
	chomp($cdsMsg);
	chomp($utrMsg);
	chomp($exonMsg);

	snpAnnotation($geneMsg,$utrMsg,$cdsMsg,$exonMsg,$reads,$coverage,\@SNP) if @SNP;
	indelAnnotation($geneMsg,$utrMsg,$cdsMsg,$exonMsg,$reads,$coverage,\@INDEL) if @INDEL;

}
close(LISTFILE);


#--------------------------
# subroutine
#--------------------------
sub indelAnnotation{
	my ($geneMsg,$utrMsg,$cdsMsg,$exonMsg,$reads,$coverage,$INDEL) = @_;
	
	my @geneSplit = split(/\s+/,$geneMsg);
	my $chr       = $seqNameMap{lc($geneSplit[0])};
	my $pos1  = $geneSplit[1];
	my $pos2  = $geneSplit[2];
	my $ratio;
	
	if($coverage>0){
		$ratio = ($reads*2)/$coverage;
	}else{
		$ratio = "NA";
	}

	my %ExonHash;
	my %UtrHash;
	my %CdsHash;
	
	my %AnnotationRecHoA;
	if ( $geneMsg!~/null/ ) {
		
		if( $exonMsg!~/null/ ){
			my @exonSplit = split( /\s+/, $exonMsg );
			my @hitGenes = split( /,/, $exonSplit[3] );
			for my $rec (@hitGenes) { $ExonHash{$rec} = 1; }
		}
		
		if ( $utrMsg!~/null/ ) {
			my @utrSplit = split( /\s+/, $utrMsg );
			my @hitGenes    = split( /,/, $utrSplit[3] );
			for my $rec (@hitGenes) { $UtrHash{$rec} = 1; }
		}
		
		if ( $cdsMsg!~/null/ ) {
			my @cdsSplit = split( /\s+/, $cdsMsg );
			my @hitGenes     = split( /,/, $cdsSplit[3] );
			for my $rec (@hitGenes) { $CdsHash{$rec} = 1; }
		}
		
		my @genes = split(/,/, $geneSplit[3]);
		for my $rec (@genes) {
			$rec =~ /(.+?)\(([+|-])\)/;
			my $geneId = $1;
			if (exists $intronMap{$geneId}) {
				for(my $i=0;$i<@{$intronMap{$geneId}};$i+=2){
					my $intronStart = $intronMap{$geneId}[$i];
					my $intronStop = $intronMap{$geneId}[$i+1];
					if( ($intronStart<$pos1 && $intronStop>$pos1) || ($intronStart<$pos2 && $intronStop>$pos2) ){
						push @{$AnnotationRecHoA{$rec}}, "Intron";
					}
				}
			}
			
			if(exists $ExonHash{$rec}){
				if(exists $UtrHash{$rec} || exists $CdsHash{$rec}){
					push @{$AnnotationRecHoA{$rec}}, "UTR" if(exists $UtrHash{$rec});
					push @{$AnnotationRecHoA{$rec}}, "CDS" if(exists $CdsHash{$rec});
				}else{
					push @{$AnnotationRecHoA{$rec}}, "non-coding";
				}
			}
		}

	} else {
		push @{$AnnotationRecHoA{"null"}}, "Intergenic";
	}
	
	# output
	my @allAnnotations;
	for my $gene ( sort {$a cmp $b} keys %AnnotationRecHoA ) {
		@allAnnotations=@{$AnnotationRecHoA{$gene}};
		$gene="" if $gene eq "null";
		print "Indel\t$chr\t$pos1\t$chr($pos1,$pos2)\t$$INDEL[0]->$$INDEL[1]\t$reads\t$coverage\t$ratio\t".join(",",@allAnnotations)."\t$gene\t\n";
	}
	
}

sub snpAnnotation{
	my ($geneMsg,$utrMsg,$cdsMsg,$exonMsg,$reads,$coverage,$SNP) = @_;
	
	my @geneSplit = split(/\s+/,$geneMsg);
	my $chr       = $seqNameMap{lc($geneSplit[0])};
	my $position  = $geneSplit[1];
	my $ratio;
	
	if($coverage>0){
		$ratio = $reads/$coverage;
	}else{
		$ratio = "NA";
	}
	
	my @AnnotationRecs;
	if ( $geneMsg!~/null/ ) {
		my @genes = split(/,/,$geneSplit[2]);
		my %intronSet;
		for my $geneRec (@genes) { $intronSet{$geneRec} = 1; } # assume intron for every hit transcript

		if ( $utrMsg!~/null/ ) {
			my @utrSplit = split( /\s+/, $utrMsg );
			my @hitGenes    = split( /,/,   $utrSplit[2] );
			for my $rec (@hitGenes) {
				delete $intronSet{$rec};
				push( @AnnotationRecs, [$rec,"UTR"] ); # a UTR hit, remove from intron list
			}
		}
		
		if ( $cdsMsg!~/null/ ) {
			my @cdsSplit = split( /\s+/, $cdsMsg );
			my @hitGenes     = split( /,/,   $cdsSplit[2] );
			for my $rec (@hitGenes) {
			        if(exists $intronSet{$rec}){
        				delete $intronSet{$rec};
        				push( @AnnotationRecs, [$rec,"CDS"] ); # a CDS hit, remove from intron list
				}
			}
		}
		
		if ( $exonMsg!~/null/ ) {
			my @exonSplit = split( /\s+/, $exonMsg );
			my @hitGenes     = split( /,/,   $exonSplit[2] );
			for my $rec (@hitGenes) {
			        if(exists $intronSet{$rec}){
			                delete $intronSet{$rec};
			                push( @AnnotationRecs, [$rec,"non-coding"] ); 
                                }
			}
		}
		
		# rest
		my @introns = keys(%intronSet);
		for my $rec (@introns) { push( @AnnotationRecs, [$rec,"Intron"] ); } # rest for intron hit

	} else {
		push( @AnnotationRecs, ["null","Intergenic"] );
	}
	
	for my $rec (@AnnotationRecs) {
		my @recArray   = @{$rec};
		my $geneStrand = $recArray[0];
		my $Annotation = $recArray[1];

		if ( $Annotation eq "CDS" ) {
			$geneStrand =~ /(.+?)\(([+|-])\)/;
			my ($cdsGene,$strand) = ($1,$2);
			my @GenomeIntervals = @{$cdsMap{$cdsGene}};
			my $GIpair          = @GenomeIntervals/2;
			my $xmerLength      = 0;
			my $xmerShift       = 0;
			my $exonSEQ         = "";
			my $LastCodon       = "";
			my $SNPCodon        = "";
			if ( $strand eq "+" ) {
				my $retrievePos;
				for my $i ( 0 .. $GIpair-1 ) { # extract CDS sequence and SNP position (frame 0) on this CDS sequence
					$i = 2 * $i;
					if ( $GenomeIntervals[$i]<=$position && $position<=$GenomeIntervals[$i+1] ) {
						$exonLength  = length($exonSEQ);
						$xmerLength  = $exonLength+$position-$GenomeIntervals[$i]+1;
						$xmerShift   = $xmerLength % 3;
						$xmerShift   = 3-$xmerShift if $xmerShift>0;
						$retrievePos = $exonLength+$position-$GenomeIntervals[$i]+1+$xmerShift;
					} else {
						$xmerLength =$GenomeIntervals[$i+1]-$GenomeIntervals[$i]+1;
					}
					$exonSEQ .= uc($genomeDB->seq($chr,($GenomeIntervals[$i]) => ($GenomeIntervals[$i+1])));
				}
				my $SNPmer = 0; 
				if ($retrievePos-12<1) {
					my $shift = 12 - $retrievePos;
					$LastCodon = substr($exonSEQ,0,21-$shift);
					$SNPmer = 2-$xmerShift+9-$shift;
				} else {
					$LastCodon = substr($exonSEQ,$retrievePos-12,21);
					$SNPmer = 2-$xmerShift+9; 
				}
				my @tmpCodon = split(//,$LastCodon);
				$tmpCodon[$SNPmer] = $$SNP[1];
				$SNPCodon = join("",@tmpCodon);
			} elsif ( $strand eq "-" ) {
				my $retrievePos;
				for my $i ( 0 .. $GIpair-1 ) {
					$i = 2*($GIpair-$i-1);
					if ( $GenomeIntervals[$i]<=$position && $position<=$GenomeIntervals[$i+1] ) {	
						$exonLength  = length($exonSEQ);
						$xmerLength  = $exonLength+$GenomeIntervals[$i+1]-$position+1;
						$xmerShift   = $xmerLength%3;
						$xmerShift   = 3-$xmerShift if $xmerShift>0;
						$retrievePos = $exonLength+$GenomeIntervals[$i+1]-$position+1+$xmerShift;
					} else {
						$xmerLength = $GenomeIntervals[$i+1]-$GenomeIntervals[$i]+1;
					}
					$exonSEQ = uc($genomeDB->seq($chr,($GenomeIntervals[$i]) => ($GenomeIntervals[$i+1]))).$exonSEQ;
				}
				my $SNPmer = 0;
				$exonSEQ = reverse($exonSEQ);
				if ($retrievePos-12<1) {
					my $shift = 12 - $retrievePos;
					$LastCodon=substr($exonSEQ,0,21-$shift);
					$SNPmer = 2-$xmerShift+9-$shift;
				}else {
					$LastCodon = substr($exonSEQ,$retrievePos-12,21);
					$SNPmer = 2-$xmerShift+9;
				}
				my @tmpCodon = split(//,$LastCodon);
				$LastCodon =~ tr/ATCG/TAGC/;
				$tmpCodon[$SNPmer] = $$SNP[1];
				$SNPCodon = join("",@tmpCodon);
				$SNPCodon =~ tr/ATCG/TAGC/;
			}
			my $Mutant = "";
			if ($debug) {
				$Mutant = "$LastCodon->$SNPCodon";
			}else{
				my $OriAA = seq2aa($LastCodon);
				my $MntAA = seq2aa($SNPCodon);
				if ( $OriAA ne $MntAA ) {
					$Mutant = "$OriAA->$MntAA";
				}else{
						$Mutant = "$OriAA";
				}
			}
			print "SNP\t$chr\t$position\t$chr:$position\t$$SNP[0]->$$SNP[1]\t$reads\t$coverage\t$ratio\t$Annotation\t$cdsGene($strand)\t$Mutant\n";
		} elsif ( $Annotation eq "UTR") {
			print "SNP\t$chr\t$position\t$chr:$position\t$$SNP[0]->$$SNP[1]\t$reads\t$coverage\t$ratio\t$Annotation\t$geneStrand\t\n";
		} elsif ( $Annotation eq "Intron" ) {
			$geneStrand =~ /(.+?)\(([+|-])\)/;
			my ($intronGene,$strand) = ($1,$2);
			my @GenomeIntervals = @{$exonMap{$intronGene}};
			my $GIpair          = @GenomeIntervals/2;
			if ( $strand eq "+" ) {
				for my $i ( 0 .. $GIpair-1 ) {
					$i = 2 * $i;
					if ( $position >= $GenomeIntervals[$i]-2 && $position <= $GenomeIntervals[$i+1]) {
						$Annotation = "Intron(Acceptor)";
					}elsif ( $GenomeIntervals[$i+1]+2 >= $position && $position > $GenomeIntervals[$i]) {
						$Annotation = "Intron(Donor)";
					}
				}
			} elsif ( $strand eq "-" ) {
				for my $i ( 0 .. $GIpair-1 ) {
					$i = 2*($GIpair-$i-1);
					if ( $GenomeIntervals[$i+1]+2 >= $position && $position > $GenomeIntervals[$i]) {
						$Annotation = "Intron(Acceptor)";
					}elsif ($position >= $GenomeIntervals[$i]-2 && $position <= $GenomeIntervals[$i+1]) {
						$Annotation = "Intron(Donor)";
					}
				}
			}
			print "SNP\t$chr\t$position\t$chr:$position\t$$SNP[0]->$$SNP[1]\t$reads\t$coverage\t$ratio\t$Annotation\t$geneStrand\t\n";
		} elsif ( $Annotation eq "non-coding") {
			print "SNP\t$chr\t$position\t$chr:$position\t$$SNP[0]->$$SNP[1]\t$reads\t$coverage\t$ratio\t$Annotation\t$geneStrand\t\n";
		}else{
			print "SNP\t$chr\t$position\t$chr:$position\t$$SNP[0]->$$SNP[1]\t$reads\t$coverage\t$ratio\t$Annotation\t\t\n";
		}
	}
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
			$AA_seq.="X";
		}
  	}
  	return $AA_seq;
}

sub getIntervals{
	my %intervalSet;
	my $cgffGene = "";
	my @intervals;
	open( CgffFILE, "$_[0]" );
	while (<CgffFILE>) {
		if (/^\>/) {
			@{$intervalSet{$cgffGene}} = @intervals if @intervals>0;
			my @t = split;
			$cgffGene = substr($t[0],1);
			@intervals = ();
			next;
		}
		my @t = split;
		push( @intervals, @t );
	}
	@{$intervalSet{$cgffGene}} = @intervals if @intervals>0;
  	return %intervalSet;
}

sub getIntrons{
	my $intervalHoA = shift;
	my %intronSet=();
	my @intervals;
	foreach my $key ( keys %{$intervalHoA} ) {
		@intervals = ();
		for(my $i=1;$i<$#{$$intervalHoA{$key}};$i++){ # eliminate the first and the last 
			push @intervals, ${$$intervalHoA{$key}}[$i];
		}
		@{$intronSet{$key}}=@intervals if @intervals>0;
	}
	return %intronSet;
}