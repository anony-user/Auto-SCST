#!/usr/bin/perl -w

use File::stat;
use File::Spec;
use File::Path;
no warnings qw(once);

$|=1;

my $debug = 0;
my $debugProc = 0;
my $debugFile = 0;

my $usage = "Usage: SeqSelector.pl [options] <idlistFile> <seqFile> <outFile>\n";
$usage .= "Options:\n";
$usage .= "   -space <X>  replacement for space characters in read IDs. (default: no replacement)\n";
$usage .= "   -slash <Y>  replacement for slash characters (/) in read IDs. (default: no replacement)\n";
$usage .= "   -idTrim <T>  trim last <T> characters of sequence IDs for hash keys. (default: 0)\n";
$usage .= "   -noPair     remove a hash key if two or more IDs generate this same key. (default: 1)\n";
$usage .= "   -mateOnly   select only mate reads. (default: 0)\n";
$usage .= "   -dump       dump lookup IDs only\n";
$usage .= "   -split <Z>  split <seqFile> into <Z> files for parallel selection (defalut: 1).\n";
$usage .= "   -tmpdir <tmpDir>  assign a directory to generate the temporary directory, for <Z> greater than 1. (default: current working directory)\n";

my $space;
my $slash;
my $dumpList=0;
my $headerTailTrimLen=0;
my $noPair=1;
my $mateOnly=0;
my $split=0;

my $startPos;
my $stopPos;

my $tmpdir;

# for debug
print "PROCESS $$ arguments: @ARGV\n" if $debugProc;

# retrieve options
my @arg_idx=(0..@ARGV-1);
for(my $idx=0; $idx<@ARGV; $idx++) {
	if ($ARGV[$idx] eq '-space') {
		$space=$ARGV[$idx+1];
		delete @arg_idx[$idx,$idx+1];
		$idx++;
	}elsif ($ARGV[$idx] eq '-slash') {
		$slash=$ARGV[$idx+1];
		delete @arg_idx[$idx,$idx+1];
		$idx++;
	}elsif ($ARGV[$idx] eq '-dump') {
		$dumpList=1;
		delete $arg_idx[$idx];
	}elsif ($ARGV[$idx] eq '-idTrim') {
		$headerTailTrimLen=$ARGV[$idx+1];
		delete @arg_idx[$idx,$idx+1];
		$idx++;
	}elsif ($ARGV[$idx] eq '-noPair') {
		$noPair=$ARGV[$idx+1];
		delete @arg_idx[$idx,$idx+1];
		$idx++;
	}elsif ($ARGV[$idx] eq '-mateOnly') {
		$mateOnly=$ARGV[$idx+1];
		delete @arg_idx[$idx,$idx+1];
		$idx++;
	}elsif ($ARGV[$idx] eq '-split') {
		$split=$ARGV[$idx+1];
		delete @arg_idx[$idx,$idx+1];
		$idx++;
	}elsif ($ARGV[$idx] eq '-segment') {
		$startPos=$ARGV[$idx+1];
		$stopPos=$ARGV[$idx+2];
		delete @arg_idx[$idx..$idx+2];
		$idx+=2;
	}elsif ($ARGV[$idx] eq '-tmpdir') {
		$tmpdir=$ARGV[$idx+1];
		delete @arg_idx[$idx,$idx+1];
		$idx++;
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;
# rest parameters
my $listFilename = shift or die $usage;
my $seqFilename = shift or die $usage;
my $outFilename = shift or die $usage;
if(defined $startPos && $debugProc){
	print "PROCESS $$ files: $listFilename $seqFilename $outFilename\n";
	print "PROCESS $$ segment: $startPos $stopPos\n";
}

# checks
die "existing output file: $outFilename\n" if (-e $outFilename);
die "no pipe seqFile ($seqFilename) for -split >1\n" if ($split>1 && (-p $seqFilename));

# read selection list
open(LISTFILE,"<$listFilename") or die "file reading error: $listFilename\n";
my %listHash=();
while(my $line=<LISTFILE>){
	$line = trim($line);
	my @token=split(/\s+/, $line);
	next if length($token[0])<=$headerTailTrimLen; # skip lines whose first token too short
	my $trimID = substr($token[0],0,length($token[0])-$headerTailTrimLen);
	$listHash{$trimID}{$token[0]}=1;
}
close LISTFILE;
# remove pairs
if($noPair){
	for my $key (keys %listHash){
		delete $listHash{$key} if (keys %{$listHash{$key}})>1;
	}
}
# count list size
my $listCnt=0;
for my $key (keys %listHash){
	$listCnt += (keys %{$listHash{$key}});
}
print "list size = ".keys(%listHash)."\n" if not defined $startPos;

# dump list hash
if($dumpList){
	for my $key1 (sort keys %listHash){
		for my $key2 (sort keys %{$listHash{$key1}}){
			print "$key1\t$key2\n";
		}
	}
	exit;
}

# split preprocessing
if($split>1){
	# open file and see if this file is FASTQ
	my $isFASTQ = 0;
	open(testSEQFILE,"<$seqFilename");
	my $firstLine = <testSEQFILE>;
	$isFASTQ = 1 if $firstLine=~/^@/;
	
	# get split start positions
	my $fileSize = stat($seqFilename)->size;
	print "file size: $fileSize\n" if $debug;
	my $share = int($fileSize/$split);
	my @position = ();
	push @position, 0;
	for(my $testIdx=1; $testIdx<$split; $testIdx++){
		my $testPos = $share*$testIdx;
		$testPos = getNextHeaderPos($isFASTQ,\*testSEQFILE,$testPos);
		push @position,$testPos if $testPos > $position[@position-1] && $testPos < $fileSize;
	}
	push @position, $fileSize; # so we have end points of all splits

	if($debugFile){
		print "positions: @position\n";
		for $testPos (@position){
			seek(testSEQFILE,$testPos,0);
			print "position $testPos\n";
			my $line = <testSEQFILE>;
			last if eof;
			$line = trim($line);
			print "HEADER: $line\n" if not eof;
			$line = <testSEQFILE>;
			$line = trim($line);
			print "LINE: $line\n" if not eof;
		}
	}
	close testSEQFILE;
	
	# prepare temp directory
	my $Proc_ID=$$;
	my ($sec, $min, $hr, $day, $mon, $year) = localtime;
	$year += 1900; $mon +=1;
	my @date_label = ("mon","day","hr","min","sec");
	(eval ("\$$_=sprintf(\"%02d\",\$$_)")) for (@date_label);
	# sfName for sequence file name, suppose no extension
	my ($volume,$directories,$sfName) = File::Spec->splitpath( $seqFilename );
	$sfName =~ s/(\.fastq$|\.fasta$|\.fna$|\.fas$|\.txt$|\.faa$|\.fa$|\.fq$)//i;
	my ($volumeTmp,$directoriesTmp,$filenameTmp);
	if (not defined $tmpdir) {
		($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( File::Spec->curdir(), 1 ); # get current directory
	}else{
		($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( $tmpdir, 1 );
	}
	$tmpdir = File::Spec->catpath( $volumeTmp, $directoriesTmp, $year.$mon.$day."_".$hr.$min."_".$Proc_ID."_$sfName" );
	mkdir($tmpdir);
	# for exact info of temp directory
	($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( $tmpdir, 1 );
	
	# fork children for splits
	my @children;
	my @tmpFasFiles;
	for(my $stopIdx=1; $stopIdx<@position; $stopIdx++){
		my ($segStart,$segStop) = @position[$stopIdx-1,$stopIdx];
		my $tmpFasOut = File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_fasOut".$stopIdx.".fasta" );
		my $cmd = "SeqSelector.pl";
		$cmd .= " -space $space" if defined $space;
		$cmd .= " -slash $slash" if defined $slash;
		$cmd .= " -idTrim $headerTailTrimLen -noPair $noPair -mateOnly $mateOnly -segment $segStart $segStop $listFilename $seqFilename";
		$cmd .= " $tmpFasOut";
		print "CMD: $cmd\n" if $debug;
		
		# fork & execute
		my $pid=fork();
		if ($pid) { # parent
			push @children, $pid;
			push @tmpFasFiles, $tmpFasOut;
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
	}
	
	# waiting for each process finished
	foreach my $child (@children) {
		waitpid($child, 0);
		die "some child process exit with error\n" if $?;
	}
	print "wait end\n" if $debug;
	
	# merge outputs
	eval{ # try un*x style merge first
		print "by cat\n" if $debug;
		# redirect possible output to STDERR of the command to STDOUT
		open(CPERR, ">&STDERR");
		open(STDERR,">&STDOUT");
		my $cmd = "cat @tmpFasFiles > $outFilename";
		my $retVal = system $cmd;
		# redirect STDERR back if execution error
		open(STDERR, ">&CPERR");
		die "execution of cat error\n" if $retVal;
	};
	if($@){ # by perl if any error
		print "by perl\n" if $debug;
		print "$@\n" if $debug;
		open(OUTFILE,">$outFilename");
		for my $tmpFasFile (@tmpFasFiles){
			open(SEQFILE,"<$tmpFasFile");
			while(<SEQFILE>){
				print OUTFILE $_;
			}
			close SEQFILE;
		}
		close OUTFILE;
	}
	
	# output count
	my $cnt = countHeader($outFilename);
	print "selected $cnt\n";

	# remove tmp directory
	if(not $debug){ # not to remove temp directory if in debug mode
		rmtree($tmpdir);
		my $tries = 0;
		while($tries < 10 && -d "$tmpdir") {
			select(undef, undef, undef, 0.75);
			rmtree($tmpdir);
			$tries++;
		}
		die "cannot remove $tmpdir\n" if (-d "$tmpdir");
	}

	exit;
}

# processing
open(SEQFILE,"<$seqFilename");
seek(SEQFILE,$startPos,0) if defined $startPos;

open(OUTFILE,">$outFilename");
my $isFASTQ = 0;
my $printFlag=0;
my $cnt=0;
while(my $line=<SEQFILE>){
	last if defined $stopPos && tell(SEQFILE)>=$stopPos;
	
	$line = trim($line);
	print "." if ($.%1000000)==0;
	
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

	if($line =~ /^>/){
		my $seqID = substr($line,1);
		my $trimID;
		# trim header tail if necessary
		if($headerTailTrimLen){
		    $trimID = substr($seqID,0,length($seqID)-$headerTailTrimLen);
		}else{
		    $trimID = $seqID;
		}
		# print check
		if(exists $listHash{$trimID}
			&& ((not $mateOnly) || (not exists $listHash{$trimID}{$seqID}) || (keys %{$listHash{$trimID}})>1)
		){
			$printFlag=1;
			$cnt++;
		}else{
			$printFlag=0;
		}
	}
	if($printFlag){
		print OUTFILE $line."\n";
	}
}
close SEQFILE;
close OUTFILE;
print "selected $cnt\n" if not defined $startPos;

sub trim {
	my $str = shift;
	$str =~ s/(^\s+|\s+$)//g;
	return $str;
}

sub getNextHeaderPos {
	my $isFASTQ = shift;
	my $fileHandle = shift;
	my $testStartPos = shift;
	my $line;
	
	# skip rest part of current line
	seek($fileHandle,$testStartPos,0);
	$line = <$fileHandle>;
	$line = trim($line);
	print "ENTER: $testStartPos\t$line\n" if $debugFile && not eof $fileHandle;
	print "EOF\n" if $debugFile && eof $fileHandle;

	if($isFASTQ){
		my $lastLineStart = tell($fileHandle);

		while(not eof $fileHandle){
			# get a first line starting with @
			while($line=<$fileHandle>){
				last if $line=~/^@/;	# break if meet possible sequence header
				$lastLineStart = tell($fileHandle);
			}
			# break if there is no such line
			last if eof $fileHandle;
			print "POS: $lastLineStart\n" if $debugFile;
			$line = trim($line);
			print "FASTQ 1st: $line\n" if $debugFile && not eof $fileHandle;
			# get next line
			$line=<$fileHandle>;
			last if eof $fileHandle; # break if there is no next line
			$line = trim($line);
			print "FASTQ 2nd: $line\n" if $debugFile && not eof $fileHandle;
			return $lastLineStart if $line=~/^[ACGTNacgtn]+$/;
		}
		$lastLineStart = tell($fileHandle); # must reach eof
		return $lastLineStart;
	}else{
		my $lastLineStart = tell($fileHandle);
		while($line=<$fileHandle>){
			last if $line=~/^>/;	# break if meet sequence header
			$lastLineStart = tell($fileHandle);
		}
		print "POS: $lastLineStart\n" if $debugFile;
		$line = trim($line);
		print "FASTA: $line\n" if $debugFile && not eof $fileHandle;
		return $lastLineStart;
	}
}

sub countHeader {
	my $filename = shift;
	my $headerCnt;

	eval{ # try un*x style merge first
		print "by grep | wc \n" if $debug;
		# redirect possible output to STDERR of the command to STDOUT
		open(CPERR, ">&STDERR");
		open(STDERR,">&STDOUT");
		$headerCnt = `grep -E \"^>\" $filename | wc -l`;
		# redirect STDERR back if execution error
		open(STDERR, ">&CPERR");
		die "execution of cat error\n" if not defined $headerCnt;
		$headerCnt = trim($headerCnt);
	};
	if($@){ # by perl if any error
		print "by perl\n" if $debug;
		print "$@\n" if $debug;
		open(FILE,">$filename");
		while(<FILE>){
			$headerCnt ++ if /^>/;
		}
		close OUTFILE;
	}

	return $headerCnt;
}
