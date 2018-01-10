#!/usr/bin/perl -w
use warnings;
use Bio::DB::Fasta;
use IPC::Open2;
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

my $usage  = "Usage: SNPannotation.pl <SNP record file> <rackj JAR path> <CDS CGFF> <UTR CGFF> <exon CGFF> <genome FASTA> [-minread <conut>] [-debug]\n";
my $SNPrec = shift or die $usage;
my $jarPath = shift or die $usage;
my $cdsCGFF = shift or die $usage;
my $utrCGFF = shift or die $usage;
my $exonCGFF = shift or die $usage;
my $FastaDB = shift or die $usage;

# query gene region by -exon false
my $pid = open2( *geneReport, *geneCheck,"java -classpath $jarPath special.GeneQuery -exon false -GFF $cdsCGFF");
my $pid2 = open2( *exonReport, *exonCheck,"java -classpath $jarPath special.GeneQuery -exon true -GFF $cdsCGFF");
my $pid3 = open2( *utrReport, *utrCheck,"java -classpath $jarPath special.GeneQuery -exon true -GFF $utrCGFF");
my $stream = Bio::DB::Fasta->new("$FastaDB")->get_PrimarySeq_stream;
my %seqDB;

# convert 'Chr1' to 'chr1'
# modified by yungi @ 2013/01/23 
while ( my $seq = $stream->next_seq ) {
	my $FasID = $seq->id;
	$FasID = lc($FasID); 
	$seqDB{$FasID} = $seq->seq();
}

my %cdsMap = getIntervals($cdsCGFF);
my %exonMap = getIntervals($exonCGFF);

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

print "#position\tmutation\t#snpReads\tcoverage\tannotation\tgene\tmutant effect\n";
my %codeMap;
open( LISTFILE, "<$SNPrec" );
while (my $line=<LISTFILE>) {
	my @t = split(/\s+/,$line);
	my @SNP;
	my $reads=0;
	my $coverage=0;
	if ($t[0] =~ /.+\(\d+,\d+\):(\d+):(\d+)/) { # INDEL pattern, to be modified
		@SNP = ($1,$2);
		$reads = $t[1];
	} elsif ($line =~ /.+:\d+:(.)->(.)\s\S*\s(\d+)\s(\d+)\s(\d+)\s(\d+)\s(\d+)\s(\d+)/) { # from NtFreqCounter
		@SNP = ($1,$2);
		$coverage=$3;
		$reads = eval "\$$codeMap{$SNP[1]}";
		$line = $t[0];
	} elsif ($t[0] =~ /.+:\d+:(.)->(.)/) { # current SNP notation
		@SNP = ($1,$2);
		$reads = $t[1];
	} elsif ($line =~ /#SNPString\sHitGenes\sReads\sFreq\((.)\)\sFreq\((.)\)\sFreq\((.)\)\sFreq\((.)\)\sFreq\((.)\)/) { # from NtFreqCounter, first line
		for $i (1..5) {
			eval "\$codeMap{\$$i}=$i+3";
		}
	}
	next if $line =~ /^#/;
	next if $SNP[1] eq 'N';
	next if $reads < $readfilter;
	$line =~ s/\s+$//;
 	print geneCheck "$t[0]\n";
	print exonCheck "$t[0]\n";
	print utrCheck "$t[0]\n";
	my $geneMsg = <geneReport>;
	my $exonMsg = <exonReport>;
	my $utrMsg  = <utrReport>;
	chomp($geneMsg);
	chomp($exonMsg);
	chomp($utrMsg);

	my @geneSplit = split(/\s+/,$geneMsg);
	my $chr       = $geneSplit[0];
	my $position  = $geneSplit[1];

	my @AnnotationRecs;
	if ( $geneMsg!~/null/ ) {
		my @genes = split(/,/,$geneSplit[2]);
		my %intronSet;
		for my $geneRec (@genes) { $intronSet{$geneRec} = 1; } # assume intron for every hit transcript

		if ( $utrMsg!~/null/ ) {
			my @utrSplit = split( /\s+/, $utrMsg );
			my @genes    = split( /,/,   $utrSplit[2] );
			for my $rec (@genes) {
				delete $intronSet{$rec};
				push( @AnnotationRecs, [$rec,"UTR"] ); # a UTR hit, remove from intron list
			}
		}

		if ( $exonMsg!~/null/ ) {
			my @exonSplit = split( /\s+/, $exonMsg );
			my @exons     = split( /,/,   $exonSplit[2] );
			for my $rec (@exons) {
				delete $intronSet{$rec};
				push( @AnnotationRecs, [$rec,"CDS"] ); # a CDS hit, remove from intron list
			}
		}

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
						$exonSEQ .= uc(substr($seqDB{$chr},$GenomeIntervals[$i]-1,$GenomeIntervals[$i+1]-$GenomeIntervals[$i]+1));
					} else {
						$xmerLength =$GenomeIntervals[$i+1]-$GenomeIntervals[$i]+1;
						$exonSEQ .= uc(substr($seqDB{$chr},$GenomeIntervals[$i]-1,$xmerLength));
					}
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
				$tmpCodon[$SNPmer] = $SNP[1];
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

						$exonSEQ = uc(substr($seqDB{$chr},$GenomeIntervals[$i]-1,$GenomeIntervals[$i+1]-$GenomeIntervals[$i]+1)).$exonSEQ;
					} else {
						$xmerLength = $GenomeIntervals[$i+1]-$GenomeIntervals[$i]+1;
						$exonSEQ = uc(substr($seqDB{$chr},$GenomeIntervals[$i]-1,$xmerLength)).$exonSEQ;
					}
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
				$tmpCodon[$SNPmer] = $SNP[1];
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
			print "$chr:$position\t$SNP[0]->$SNP[1]\t$reads\t$coverage\t$Annotation\t$cdsGene($strand)\t$Mutant\n";
		} elsif ( $Annotation eq "UTR") {
			print "$chr:$position\t$SNP[0]->$SNP[1]\t$reads\t$coverage\t$Annotation\t$geneStrand\t\n";
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
			print "$chr:$position\t$SNP[0]->$SNP[1]\t$reads\t$coverage\t$Annotation\t$geneStrand\t\n";
		}else{
			print "$chr:$position\t$SNP[0]->$SNP[1]\t$reads\t$coverage\t$Annotation\t\n";
		}
	}
}
close(LISTFILE);

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