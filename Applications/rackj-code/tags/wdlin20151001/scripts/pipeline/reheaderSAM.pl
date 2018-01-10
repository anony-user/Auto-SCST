#!/usr/bin/perl -w

$usage = "Usage: reheaderSAM.pl <header source SAM> <input SAM>\n";

my $SQsource = shift or die $usage;
my $inputSAM = shift or die $usage;

# get SQ headers
my @SQlines;
open(FILE,"<$SQsource");
while (<FILE>) {
	if (/^\@/) {  # SAM header lines
		push(@SQlines, $_) if /^\@SQ/;
	}else{
		last; #end of header section
	}
}
close FILE;

# replace
open(FILE,"<$inputSAM");
my $fhPos;
# before @SQ section
$fhPos = tell(FILE);
while(<FILE>){
	if(/^\@SQ/){
		seek(FILE,$fhPos,0);
		last;
	}else{
		print;
	}
	$fhPos = tell(FILE);
}

# replace
$fhPos = tell(FILE);
while(<FILE>){
	if(/^\@SQ/){
		# do nothing
	}else{
		seek(FILE,$fhPos,0);
		last;
	}
	$fhPos = tell(FILE);
}
for my $line (@SQlines){
	print $line;
}

# after @SQ section
while(<FILE>){
	print;
}
close FILE;
