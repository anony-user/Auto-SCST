#!/usr/bin/perl -w


$usage = "Usage: sam_filter.pl [-ID idThreshold] <passedSAM> <filteredSAM>\n";

my $ID=0.9;
my $msg="";

#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-ID') {
		$ID=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-msg') {
		$msg=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

my $Mapped   = shift or die $usage;
my $Unmapped = shift or die $usage;

open(MappedSam,">$Mapped");
open(UnmappedSam,">$Unmapped");
my $PERV_read='';
my $Mapped_read='';
my @SamBuffer;
my $outCount=0;
my $readnum=0;

my $initialHeader = 1;

while (<STDIN>) {
	if (/^\@/) {  # SAM Header output 
	    if($initialHeader){
            print MappedSam $_; 
            print UnmappedSam $_; 
        }
        next;
	}
	$initialHeader=0;
	
	$line = $_;
	my @t = split;
	
	if ($t[1]&4) {  #Filter flag: 4->read unmapped, 8->mate unmapped
		print UnmappedSam $line; #Unmapped read
		$PERV_read=$t[0];
		$readnum ++;
		next;
	}
	
	if (@SamBuffer == 0 ) {
		push (@SamBuffer,$line);
	}elsif ($t[0] eq $PERV_read) {
		push (@SamBuffer,$line);  # put sam records to SamBuffer with the same READ ID
	}else{ # meet different read ID, start buffer processing
		# Start buffer processing, do nothing to current read line 
		$readnum ++;
		my @PassIDs = filterCore(@SamBuffer);
		if (@PassIDs == 0) {
			print UnmappedSam $SamBuffer[0];
		}else{
			(print MappedSam $SamBuffer[$_]) for @PassIDs;
			$outCount ++;
		}
		# Add current line to SamBuffer
		@SamBuffer=();
		push (@SamBuffer,$line);
	}
	$PERV_read=$t[0];
}
#process last record buffer
if (@SamBuffer>0) {
	$readnum ++;
	my @PassIDs = filterCore(@SamBuffer);
	if (@PassIDs == 0) {
		print UnmappedSam $SamBuffer[0];
	}else{
		(print MappedSam $SamBuffer[$_]) for @PassIDs;
		$outCount++;
	}
}

close(MappedSam);
close(UnmappedSam);

if (length($msg)>0) {
	print "($msg) SAM ID filter ($ID) reads: $readnum -> $outCount\n";
}

sub filterCore{
	my @SamBuffer = @_;
	my @SamIDs=();
	for my $i (0 ... @SamBuffer-1) {
		@St = split(/\t/,$SamBuffer[$i]);
		my $length = length($St[9]); # Sequence length from SEQ
		my $CIGAR = $St[5];
		$CIGAR =~ s/\d+[HSIDNP]//g;
		$CIGAR =~ s/[M]/\t/g;      # Match+Mismatch length from CIGAR
		my @r = split(/\t/,$CIGAR);
		my $match=0;
		($match+=$_) for @r; 
		if ($SamBuffer[$i] =~ /XM:i:\d+/) {
			$SamBuffer[$i] =~ /XM:i:(\d+)/;  # number of mismatch
			my $XM = $1;
			$match -= $XM;
		}elsif ($SamBuffer[$i] =~ /NM:i:\d+/) {
			$SamBuffer[$i] =~ /NM:i:(\d+)/;  # number of mismatch
			my $NM = $1;
			$match -= $NM;
		}
		$SamIDs[$i] = $match/$length;
	}
	my @PassIDs=();
	for my $j (0 ... @SamIDs-1) { # filtering Identies
		push (@PassIDs,$j) if $SamIDs[$j]>=$ID;
	}
  	return @PassIDs;
}