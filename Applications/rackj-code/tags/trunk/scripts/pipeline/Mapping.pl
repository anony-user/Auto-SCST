#!/usr/bin/perl
use File::Spec;
use File::Copy;
use File::Path;
use Bio::DB::Fasta;

my $debug = 0;

BEGIN {unshift(@INC,$ENV{'INC'}) if defined $ENV{'INC'};}

my ($space,$slashF,$split,$keep,$samsort,$showcmd)=('_','_',1,0,'sortname',0);
my $tmpdir='';
my $reference='';
$|=1;

#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-space') {
		$space=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-slash') {
		$slashF=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-split') {
		$split=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-keep') {
		$keep=1;
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-tmpdir') {
		$tmpdir=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-sortname') {
		$samsort="sortname";
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-sortpos') {
		$samsort="sortpos";
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-nosort') {
		$samsort='nosort';
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-table') {
		$ENV{'TargetTableFile'}=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-reference') {
		$reference=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-showcmd') {
		$showcmd=1;
		print "Mapping.pl ".join(" ",@ARGV)."\n";
		delete $arg_idx[$i];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

$usage  = "Usage: Mapping.pl [options] <target> <query> <SAMfile> <MethodScript> <method parameters>\n";
$usage .= "Options:\n";
$usage .= "   -space <X>  replacement for space characters in read IDs. (default: _)\n";
$usage .= "   -slash <Y>  replacement for slash characters (/) in read IDs. (default: _)\n";
$usage .= "   -split <Z>  split query to <Z> files. (defalut: 1).\n";
$usage .= "   -table <altTable>  assign an alternative mapping table.\n";
$usage .= "   -tmpdir <tmpDir>   assign a directory to generate the temporary directory. (default: current working directory)\n";
$usage .= "   -keep       keep the temporary directory and intermediate files.\n";
$usage .= "   -sortname   sort output SAM/BAM file by read IDs. This is the default sorting order.\n";
$usage .= "   -sortpos    sort output SAM/BAM file by alignment coordinates.\n";
$usage .= "   -nosort     no sorting of output SAM/BAM file.\n";
$usage .= "   -reference  Set headers using given reference sequence file\n";
$usage .= "   -showcmd    show mapping command.\n";
my $Target = shift or die $usage;
my $Query = shift or die $usage;
my $SamOut = shift or die $usage;
my $Method = shift or die $usage;
my $ParaLine = join(' ', @ARGV);

die "no -SamTmp allowed in <method parameters> when -split greater than 1\n" if $split>1 && ($ParaLine=~/^-SamTmp/ || $ParaLine=~/\s-SamTmp/);
die "no space allowed in query filename\n" if $Query=~/\s/;

# check target FASTA index build
if ($Method =~ /Blat/) {
	my @BlatTargets;
	if ($Method eq "MappingBlat.pl") {
		@BlatTargets = $ParaLine =~ /-target\s+(\S+)/;
	}else{
		@BlatTargets = $ParaLine =~ /-blat[\d| ].*?-target\s+(\S+)/g;
	}
	require("MappingTargetTable.pl");
	push (@BlatTargets,MappingTargetTable($Target,"blat"));
	for my $fasfile (@BlatTargets) {
		if (defined $fasfile) {
			my $db = Bio::DB::Fasta->new($fasfile);
			my @IDs = $db->get_all_ids;
			$db = Bio::DB::Fasta->new($fasfile,'-reindex',1) if @IDs == 0;
		}
	}
}

my $Proc_ID=$$;
my ($sec, $min, $hr, $day, $mon, $year) = localtime;
$year += 1900; $mon +=1;
@date_label = ("mon","day","hr","min","sec");
(eval ("\$$_=sprintf(\"%02d\",\$$_)")) for (@date_label);

my ($volume,$directories,$QueryName) = File::Spec->splitpath( $Query );
$QueryName =~ s/(\.fastq$|\.fasta$|\.fna$|\.fas$|\.txt$|\.faa$|\.fa$|\.fq$)//i;
if ($tmpdir eq '') {
	my ($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( File::Spec->curdir(), 1 ); #assuming no file
	$ENV{'ALN_TMP'} = File::Spec->catpath( $volumeTmp, $directoriesTmp, $year.$mon.$day."_".$hr.$min."_".$Proc_ID."_$QueryName" );
}else{
	my ($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( $tmpdir, 1 ); #assuming no file
	$ENV{'ALN_TMP'} = File::Spec->catpath( $volumeTmp, $directoriesTmp, $year.$mon.$day."_".$hr.$min."_".$Proc_ID."_$QueryName" );
}
$tmpdir = $ENV{'ALN_TMP'};
my ($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( $tmpdir, 1 ); #assuming no file

print "DEBUG: tmp dir: $tmpdir\n" if $debug;
print "DEBUG: volume: $volumeTmp\n" if $debug;
print "DEBUG: directories: $directoriesTmp\n" if $debug;

# count read number
my $FirstLine;
my $count;
open(FILE, $Query) or die("Error Query file\n");
$FirstLine = readline( *FILE );  # Get first line to tell apart FASTA or FASTQ
if ($FirstLine =~ /^\@/) {
	eval{ # try un*x wc first
		print "by wc\n" if $debug;
		# redirect possible output to STDERR of the command to STDOUT
		open(CPERR, ">&STDERR");
		open(STDERR,">&STDOUT");
		$count= `wc -l $Query`;
		# redirect STDERR back if execution error
		open(STDERR, ">&CPERR");
		die "execution of wc error\n" if not defined $count;
		$count =~ s/\s.+//;
		$count = $count/4;
	};
	if($@){ # count by perl if any error
		print "by perl\n" if $debug;
		print "$@\n" if $debug;
		$count++;
		while (<FILE>) {
			$count++;
		}
		$count = $count/4;
	}
}elsif ($FirstLine =~ /^\>/) {
	$count++;
	while (<FILE>) {
		$count++ if /^\>/;
	}
}
close(FILE);


# calculate how many reads in a FASTA file
my $NumSplitReads=0;
if ($split>1) {
	$NumSplitReads=int $count/$split;
	my $remains = $count%$split;
	$NumSplitReads++ if $remains>0;
}

mkdir($tmpdir);

my @FASTAs;
print "Split processing..." if $split>1;
open(FILE, $Query) or die("Error opening query file\n");
my $outFileNum = 0;
my $printedReadNum = 0;
my $FasQ=($FirstLine =~ /^\@/)? 1 : 0;
if($split==1) {
	$outFileNum++;
	my $NewFastA=File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_Query".$outFileNum.".fasta" );
	print "DEBUG: new FASTA: $NewFastA\n" if $debug;
	open(OUTFILE,">$NewFastA");
	push (@FASTAs,$NewFastA);
}

while( <FILE> )
{
	if ($FasQ) {                         # 
		next if $.%4==3 || $.%4==0;      #  Header processing
		s/@/>/;                          #  1. FASTQ (@) -> FASTA (>)
	}                                    #  2. space ' ' -> '_' or user defined
	s/\ /$space/g;                       #  3. slash '/' -> '_' or user defined
	s/\//$slashF/g;                      # 

	if (/^\>/ && $split>1) {
		$printedReadNum++;
		if((($printedReadNum % $NumSplitReads) == 1 && $NumSplitReads > 1) || ( $NumSplitReads == 1)){
			if($outFileNum > 0){
				close OUTFILE;
			}
			$printedReadNum = 1;
			$outFileNum++;
			my $NewFastA=File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_Query".$outFileNum.".fasta" );
			print "DEBUG: new FASTA: $NewFastA\n" if $debug;
			open(OUTFILE,">$NewFastA");
			push (@FASTAs,$NewFastA);
		}
	}
	die "\nQuery file possible format error at line $.\n" if (/^\>/ && length($_)>200) || ($_ !~ /^\>/ && $_ !~/[ATGCNU]+/i);
	print OUTFILE $_;
} 
close OUTFILE;
close FILE;
print "done\n" if $split>1;
$split = $outFileNum if $split>$outFileNum;

my @children;
my @SAMs;
print "Total query reads: $count\n";

if ($split==1) {  # read FASTA no split
	my $NewFastA=$FASTAs[0];
	my $TmpSAM=File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_Samtmp.sam" );
	print "DEBUG: new TMPSAM: $TmpSAM\n" if $debug;
	$cmd="$Method $Target $NewFastA $TmpSAM $ParaLine";
	push (@SAMs,$TmpSAM);
	print "$cmd\n" if $showcmd;
	my $retVal = system($cmd);
	
	die "executing \"$cmd\" error\n" if $retVal;
}else{           # read FASTA split to N files
	for my $i (1..$split) {
		my $pid = fork(); # Start fork();
		if ($pid) { # parent
			push @children, $pid;
			my $TmpSAM=File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_Samtmp".$i.".sam" );
			print "DEBUG: parent new TMPSAM: $TmpSAM\n" if $debug;
			push (@SAMs,$TmpSAM);
		} elsif (not defined $pid) {
			print STDERR "Couldn't fork number $i.\n";
		} elsif ($pid == 0) { # child
			my $NewFastA=$FASTAs[$i-1];
			my $TmpSAM=File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_Samtmp".$i.".sam" );
			print "DEBUG: child new TMPSAM: $TmpSAM\n" if $debug;
			$cmd="$Method $Target $NewFastA $TmpSAM $ParaLine";
			print "$cmd\n" if $showcmd;
			exec $cmd or print STDERR "Couldn't exec: $cmd";
			exit;
		}
	}
	
	foreach my $child (@children) {  # waiting for each process finished.
		waitpid($child, 0);
		die "some child process exit with error\n" if $?;
	}
}

# count SAM output read number
my $SAMcountTotal=0;
my @SAMcounts;
for my $fileCount (@SAMs) {
	my $PREV_SAM='';
	my $readCnt=0;
	open(samFILE, $fileCount) or die("Error Sam output file\n");
	while(<samFILE>){
		next if /^\@/;
		my @t = split;
		if($t[0] ne $PREV_SAM){
			$SAMcountTotal++;
			$readCnt++;
		}
		$PREV_SAM=$t[0];
	}
	close(samFILE);
	push @SAMcounts,$readCnt;
}

# remove empty SAM files, if any
for(my $idx=@SAMcounts-1;$idx>=0;$idx--){
	splice @SAMs,$idx,1 if $SAMcounts[$idx]==0;
}

# detect $SamOut format SAM or BAM
my $format="bam";
if ($SamOut =~ /(.+)\.sam$/i) {
	$format="sam";
}elsif ($SamOut =~ /(.+)\.bam$/i) {
	$format="bam";
}
my $SamOutPrefix = $1;
$SamOutPrefix = $SamOut if length($SamOutPrefix)==0;

# Merge all sam results
if($SAMcountTotal){
	$cmd="MergeSam2.pl -$samsort -format $format $SamOutPrefix @SAMs ";
	$cmd.="-reference $reference " if $reference ne '';
	print "$cmd\n" if $showcmd;
	my $retVal = system($cmd);
	
	die "executing \"$cmd\" error\n" if $retVal;
	
	move("$SamOutPrefix.$format",$SamOut) if "$SamOutPrefix.$format" ne $SamOut;
}

print "Total SAM output reads: $SAMcountTotal\n";

if ($keep==0) {
	rmtree($tmpdir);
	my $tries = 0;
	while($tries < 10 && -d "$tmpdir") {
		select(undef, undef, undef, 0.75);
		rmtree($tmpdir);
		$tries++;
	}
	print "Couldn't remove $tmpdir\n" if (-d "$tmpdir");
}
