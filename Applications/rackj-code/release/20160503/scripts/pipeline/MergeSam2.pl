#!/usr/bin/perl -w

use File::Spec;
use File::Copy;

my $debug=0;

my $samSortOpt = "-m 2000000000";

my $samsort='name';
my $format='bam';
my $reference='';
my $reheader = 0;
#$showcmd = $ENV{'ShowCMD'} if defined $ENV{'ShowCMD'};
#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-sortname') {
		$samsort='name';
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-sortpos') {
		$samsort='position';
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-nosort') {
		$samsort='nosort';
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-format') {
		$format=lc($ARGV[$i+1]);
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-reference') {
		$reference=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-reheader') {
		$reheader=1;
		delete $arg_idx[$i];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

$usage = "Usage: MergeSam2.pl [options] <Output prefix> <Sam File1>...<Sam FileN>\n";
$usage .= "Options:\n";
$usage .= "   -sortname   Sort alignments by read names. This is the default sorting order.\n";
$usage .= "   -sortpos    Sort alignments by leftmost coordinates.\n";
$usage .= "   -nosort     no sorting of output SAM/BAM file.\n";
$usage .= "   -format     Set output format SAM or BAM. <sam|bam> (Default:bam)\n";
$usage .= "   -reference  Set headers using given reference sequence file\n";
$usage .= "   -reheader   Unify \@SQ headers for all files using the first SAM.\n";
die $usage if @ARGV < 2;
$tmpdir = $ENV{'ALN_TMP'} if exists $ENV{'ALN_TMP'};
$tmpdir = File::Spec->tmpdir() if !exists $ENV{'ALN_TMP'};
my ($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( $tmpdir, 1 ); #assuming no file
$Output = shift;

if($samsort eq "nosort"){
	my ($nullVol,$nullDir,$tmpFile)=File::Spec->splitpath($Output); # get filename portion
	my $tmpSAM = File::Spec->catpath( $volumeTmp, $directoriesTmp, "$tmpFile.tmp.sam" ); # form a filepath under tmpdir
	$cmd="MergeSam.pl @ARGV > $tmpSAM";
	print "$cmd\n" if $debug;
	my $retVal = system($cmd);
	die "executing \"$cmd\" error\n" if $retVal;
	
	if($format eq "bam"){
		$cmd="samtools view -S -b -o $Output.bam $tmpSAM";
		print "$cmd\n" if $debug;
		# redirect possible output to STDERR of the command to STDOUT
		open(CPERR, ">&STDERR");
		open(STDERR,">&STDOUT");
		my $retVal = system($cmd);
		die "executing \"$cmd\" error\n" if $retVal;
		# redirect STDERR back
		open(STDERR, ">&CPERR");
	}else{ # format is sam
		move("$tmpSAM","$Output.sam");
	}
	exit;
}

my @FileSet1=@ARGV;
my $firstSAM = $FileSet1[0];
my $nullpath = File::Spec->devnull();
# convert SAM to BAM for merge and sorting
my @tmpBAMs = ();
my @children;
for (@FileSet1) {
	my $inSAM = $_;
	s/\.sam$//i;
	my ($nullVol,$nullDir,$tmpFile)=File::Spec->splitpath($_);
	my $cmd = "";
	$cmd .= "reheaderSAM.pl $firstSAM $inSAM | " if $reheader;
	$cmd .= "samtools view -S -b";
	$cmd .= " -T $reference" if $reference ne '';
	$cmd .= " -o ".File::Spec->catpath( $volumeTmp, $directoriesTmp, "$tmpFile.bam" );
	if($reheader){
		$cmd .= " /dev/stdin";
	}else{
		$cmd .= " $inSAM";
	}
	print $cmd."\n" if $debug;
	
	# fork for every to-BAM process
	my $pid=fork();
	if ($pid) { # parent
		push @children, $pid;
	} elsif (not defined $pid) {
		print STDERR "Couldn't fork for command: $cmd\n";
	} elsif ($pid == 0) { # child
		# redirect possible output to STDERR of the command to STDOUT
		open(CPERR, ">&STDERR");
		open(STDERR,">&STDOUT");
		{exec $cmd;}
		# redirect STDERR back if execution error
		open(STDERR, ">&CPERR");
		exit 1;
	}
	
	push @tmpBAMs, File::Spec->catpath( $volumeTmp, $directoriesTmp,"$tmpFile.bam" );
}

# wait children exit
foreach my $child (@children) {  # waiting for each process finished.
	waitpid($child, 0);
	die "some child process exit with error\n" if $?;
}

# Merge step
my $tmpOutput;
my $AppendBams = "";
for(my $idx=0; $idx<@tmpBAMs; $idx++){
	$AppendBams .= " $tmpBAMs[$idx]";
}
if (@ARGV >1) {
	#merge BAM files
	my ($nullVol,$nullDir,$tmpFile)=File::Spec->splitpath($Output); # get filename portion
	$tmpOutput=File::Spec->catpath( $volumeTmp, $directoriesTmp, "$tmpFile.tbam" );
	my $cmd = "samtools cat -o $tmpOutput $AppendBams";
	print $cmd."\n" if $debug;
	# redirect possible output to STDERR of the command to STDOUT
	open(CPERR, ">&STDERR");
	open(STDERR,">&STDOUT");
	my $retVal = system($cmd);
	die "executing \"$cmd\" error\n" if $retVal;
	# redirect STDERR back
	open(STDERR, ">&CPERR");
}else{
	$tmpOutput = $tmpBAMs[0];
}

# Sort step + Output step
# redirect possible output to STDERR of the command to STDOUT
open(CPERR, ">&STDERR");
open(STDERR,">&STDOUT");
if ($format eq 'bam') {
	if ($samsort eq 'name') {
		my $cmd = "samtools sort $samSortOpt -n $tmpOutput $Output";
		print $cmd."\n" if $debug;
		my $retVal = system($cmd);
		die "executing \"$cmd\" error\n" if $retVal;
	}elsif ($samsort eq 'position') {
		my $cmd = "samtools sort $samSortOpt $tmpOutput $Output";
		print $cmd."\n" if $debug;
		my $retVal = system($cmd);
		die "executing \"$cmd\" error\n" if $retVal;
	}
}elsif ($format eq 'sam'){
	my $sortedPrefix = $tmpOutput;
	$sortedPrefix =~ s/\.\w+$//;
	if ($samsort eq 'name') {
		my $cmd = "samtools sort $samSortOpt -n $tmpOutput $sortedPrefix";
		print $cmd."\n" if $debug;
		my $retVal = system($cmd);
		die "executing \"$cmd\" error\n" if $retVal;
	}elsif ($samsort eq 'position') {
		my $cmd = "samtools sort $samSortOpt $tmpOutput $sortedPrefix";
		print $cmd."\n" if $debug;
		my $retVal = system($cmd);
		die "executing \"$cmd\" error\n" if $retVal;
	}
	my $cmd = "samtools view $sortedPrefix.bam -h -o $Output.sam";
	print $cmd."\n" if $debug;
	my $retVal = system($cmd);
	die "executing \"$cmd\" error\n" if $retVal;
}
# redirect STDERR back
open(STDERR, ">&CPERR");
