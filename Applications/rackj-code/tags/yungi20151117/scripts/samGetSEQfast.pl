#!/usr/bin/perl -w
use strict;

my $debugFile = 0;

my $usage = "Usage: samGetSEQfast.pl [-unmap] [-space <X>] [-slash <Y>] <inSAM> <FASTA>+\n";

my $unmap=0;
my $space;
my $slash;

#Retrieve optional parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-unmap') {
		$unmap=1;
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-space') {
		$space=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
		$i++;
	}elsif ($ARGV[$i] eq '-slash') {
		$slash=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
		$i++;
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

my $samfile = shift or die $usage;
die $usage if @ARGV < 1;

$samfile = "-" if $samfile eq "STDIN";
my $samFILEHANDLE;
open($samFILEHANDLE, $samfile) or die("Error: cannot open sam file '$samfile'\n");

# get first line in inSAM
my $currentSamRec=getCurrentSAMRec($samFILEHANDLE);
if (@$currentSamRec){
	# skip header lines
	while(@$currentSamRec && $$currentSamRec[0] =~ /^@/){
		print join("\t",@$currentSamRec)."\n";
		$currentSamRec=getCurrentSAMRec($samFILEHANDLE);
	}
}else{
	exit;
}

# parse FASTA
for my $i ( 0 ... @ARGV - 1 ) {
	my $isFASTQ = 0;
	my $SEQId="";
	my $SEQ="";
	
	open(my $FASTAstream, $ARGV[$i]) or die("Error: cannot open FASTA file '$ARGV[$i]'\n");
	while (my $line = <$FASTAstream>) {
		$line = trim($line);
		print "." if ($.%1000000)==0 && $debugFile;
		
		if($.==1){
			$isFASTQ=1 if $line=~/^\@/;
			print "FIRST LINE: $line\n" if $debugFile;
		}
		
		if ($isFASTQ) {
			next if $.%4==3 || $.%4==0;      #  Header processing
			$line=~s/@/>/;	                 #  1. FASTQ (@) -> FASTA (>)
		}
		
		$line=~s/\ /$space/g if defined $space;  # header replacement, the same with Mapping.pl
		$line=~s/\//$slash/g if defined $slash;
		
		if ($line =~ /^\>/){
			if($SEQId ne ""){
				if (@$currentSamRec && $SEQId eq $$currentSamRec[0]) {
					samFill($SEQId, $SEQ, $samFILEHANDLE, \$currentSamRec);
				}else{
					outSeqUnalign($SEQId, $SEQ) if ($unmap);
				}
			}
			
			$SEQId = substr($line,1);
			$SEQ = "";
		}else{    
			$SEQ .= $line;
		}
	}

	# handle the last sequence
	if (@$currentSamRec && $SEQId eq $$currentSamRec[0]) {
		samFill($SEQId, $SEQ, $samFILEHANDLE, \$currentSamRec);
	}else{
		outSeqUnalign($SEQId, $SEQ) if ($unmap);
	}

	close $FASTAstream;
}

close $samFILEHANDLE;


#-------------------------------
# subroution
#-------------------------------
sub getCurrentSAMRec{
	my $fh = shift;
	
	my @ss;
	if (defined (my $sline = <$fh>)){
		@ss = split("\t",trim($sline));
	}else{
		@ss = ();
	}
	
	return \@ss;
}

sub samFill{	
	my ($seqId,$sequence,$fh,$sam) = @_;
	
	while(@{$$sam} && $seqId eq ${$$sam}[0]){
		${$$sam}[9] = $sequence;
		${$$sam}[9] =~ tr/ATCGatcg/TAGCtagc/ if ${$$sam}[1] & 16;
		${$$sam}[9] = reverse ${$$sam}[9] if ${$$sam}[1] & 16;
		${$$sam}[5] =~ tr/H/S/;
		print join( "\t", @{$$sam} )."\n";

		$$sam = getCurrentSAMRec($fh);
	}
}

sub outSeqUnalign{	
	my ($seqId,$sequence) = @_;
	
	my @unalign = ($seqId,4,"*",0,255,"*","*",0,0,$sequence,"*");
	print join( "\t", @unalign )."\n";
}


sub trim {
	my $str = shift;
	$str =~ s/\s+$//g;
	$str =~ s/^\s+//g;
	return $str;
}