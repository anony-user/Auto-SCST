#!/usr/bin/perl -w

BEGIN { unshift @INC, $ENV{'INC'} if defined $ENV{'INC'}; }

use strict;
use File::Spec;
use File::Path;
use Getopt::Long;
use Bio::DB::Sam;
use IPC::SysV qw(IPC_PRIVATE S_IRUSR S_IWUSR IPC_CREAT IPC_NOWAIT S_IRWXU);
use IPC::Semaphore;
use RackjPerlLib::Format::LIST;
use RackjPerlLib::Format::FASTA;
use RackjPerlLib::Format::FASTQ;
use RackjPerlLib::Format::SAM;
use RackjPerlLib::Format::BAM;

my $debug=0;

use constant lf => "list"; # list file
use constant sf => "seq";  # sequence file

my $usage = "Usage:	SplitSelector.pl [options] <listFile format {LIST|FASTA|FASTQ|SAM|BAM}> <listFile> <seqFile format {FASTA|FASTQ|SAM|BAM}> <seqFile> <outputFile> \n";
$usage .= "Options:\n";
$usage .= "   --idBy ".lf."=\"<ID|SEQ>\" \n";
$usage .= "   --idBy ".sf."=\"<ID|SEQ|PROG \\\"...\\\">\" \n"; 
$usage .= "   --space ".lf."=\"<w>\"		replace space characters in read IDs with <w> on listFile. (default: no replacement)\n";
$usage .= "   --space ".sf."=\"<w>\"		replace space characters in read IDs with <w> on seqFile. (default: no replacement)\n";
$usage .= "   --slash ".lf."=\"<x>\"		replace slash characters (/) in read IDs with <x> on listFile. (default: no replacement)\n";
$usage .= "   --slash ".sf."=\"<x>\"		replace slash characters (/) in read IDs with <x> on seqFile. (default: no replacement)\n";
$usage .= "   --idTrim <y>			trim last <y> characters of sequence IDs for hash keys. (default: 0)\n";
$usage .= "   --noPair			remove a hash key if two or more IDs generate this same key. (default: 0)\n";
$usage .= "   --mateOnly			select only mate reads. (default: 0)\n";
$usage .= "   --report <RAW|FASTA|ID|SEQ>\n";
$usage .= "   --split <z>			split seqFile into <z> files for parallel selection (defalut: 1).\n";
$usage .= "   --noSplitSameID		not to split same sequence ID (default: 1)\n";
$usage .= "   --tmpdir <dir>		assign a directory to generate the temporary directory, for <z> greater than 1. (default: current working dir)\n";
$usage .= "   --dump			dump lookup IDs only (default: no dump)\n";

# get options
my %idBy=(
	lf() => "ID",
	sf() => "ID"
);
my %space;
my %slash;
my $headerTailTrimLen=0; 
my $noPair=0;
my $mateOnly=0;
my $reportType = "RAW";
my $split=1;
my $noSplitSameID=1;
my $tmpdir;
my $dumpList=0;
my $progString;

GetOptions("idBy=s" => \%idBy, "space=s" => \%space, "slash=s" => \%slash, 
	"idTrim=i" => \$headerTailTrimLen, "noPair=s" => \$noPair, "mateOnly=s" => \$mateOnly, 
	"report=s" => \$reportType, "split=i" => \$split, "noSplitSameID=s" => \$noSplitSameID, "tmpdir=s" => \$tmpdir, 
	"dump" => \$dumpList)
	or die $usage;

$progString = $1 if($idBy{sf()} =~ /^PROG\s\"(.+)\"$/);

my $listFileFormat = shift or die $usage;
my $listFile = shift or die $usage;
my $seqFileFormat = shift or die $usage;
my $seqFile = shift or die $usage;
my $outputFile = shift or die $usage;

$listFileFormat = uc $listFileFormat;
$seqFileFormat = uc $seqFileFormat;

my %modules = (
	LIST => 'RackjPerlLib::Format::LIST',
	FASTA => 'RackjPerlLib::Format::FASTA',
	FASTQ => 'RackjPerlLib::Format::FASTQ',
	SAM => 'RackjPerlLib::Format::SAM',
	BAM => 'RackjPerlLib::Format::BAM',
);

die "invalid list file format\n" if(not exists $modules{$listFileFormat});
die "invalid sequence file format\n" if(not exists $modules{$seqFileFormat});
die "existing output file: $outputFile\n" if (-e $outputFile);
die "no listFile: $listFile\n" unless (-e $listFile);
die "no seqFile: $seqFile\n" unless (-e $seqFile);


#-------------------------------
# read list file
#-------------------------------
my %listHash;
my $listInstance = $modules{$listFileFormat}->new($listFile);
while( $listInstance->getNextRecord() ){
	my $sid = ($idBy{lf()} eq "ID")? $listInstance->getID() : $listInstance->getSEQ();
	next if not defined $sid || length($sid)<=$headerTailTrimLen;
	
	my $id = id_replacement($sid,lf);
	my $trimmed = trim_id($id);
	$listHash{$trimmed}{$id}=1;
}
$listInstance->closeFile();

# remove pairs
if($noPair){
	for my $key (keys %listHash){
		delete $listHash{$key} if (keys %{$listHash{$key}})>1;
	}
}

# mateOnly
if($mateOnly){
	for my $key (keys %listHash){
		delete $listHash{$key} if (keys %{$listHash{$key}})!=2;
	}
}

# count list size
my $listCnt = (keys %listHash);
print "list size = ".$listCnt."\n";

# dump list hash
if($dumpList){
	for my $key1 (sort keys %listHash){
		for my $key2 (sort keys %{$listHash{$key1}}){
			print "$key1\t$key2\n";
		}
	}
	exit;
}


#-------------------------------
# processing
#-------------------------------
my $splitterInstance = $modules{$seqFileFormat}->new($seqFile);
my $segmentHoA = $splitterInstance->getSplitPosition($split, $noSplitSameID);
my $segmentCount = keys %{$segmentHoA};

if($segmentCount<=1){
	my $k = (keys %{$segmentHoA})[0];
	my ($segStart,$segStop) = @{$$segmentHoA{$k}};
	my $cnt = seqSelector(\%listHash, $seqFile, $seqFileFormat, $segStart, $segStop, $outputFile, $reportType);
	print "selected size = $cnt\n"; 
	exit;
}

# prepare temp directory
my $Proc_ID=$$;
my ($sec, $min, $hr, $day, $mon, $year) = localtime;
$year += 1900; $mon +=1;
my @date_label = ("mon","day","hr","min","sec");
(eval ("\$$_=sprintf(\"%02d\",\$$_)")) for (@date_label);
# sfName for sequence file name, suppose no extension
my ($volume,$directories,$sfName) = File::Spec->splitpath( $seqFile );
$sfName =~ s/(\.fastq$|\.fasta$|\.fna$|\.fas$|\.txt$|\.faa$|\.fa$|\.fq$|\.bam$|\.sam$)//i;
my ($volumeTmp,$directoriesTmp,$filenameTmp);
if (not defined $tmpdir) {
	($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( File::Spec->curdir(), 1 ); # get current directory
}else{
	($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( $tmpdir, 1 );
}
$tmpdir = File::Spec->catpath( $volumeTmp, $directoriesTmp, $year.$mon.$day."_".$hr.$min."_".$Proc_ID."_$sfName" );
mkpath($tmpdir);
# for exact info of temp directory
($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( $tmpdir, 1 );

# create semaphore 
my $sem = IPC::Semaphore->new(IPC_PRIVATE, 1, S_IRUSR | S_IWUSR | IPC_CREAT) or die "IPC::Semaphore->new: $!";
$sem->setall( (1) ) || die "can't set semaphore $!";
pipe my $reader, my $writer;
$writer->autoflush(1);

# fork children for splits
my @children;
my @tempFiles;
for my $k (sort {$a<=>$b} keys %{$segmentHoA}){
	my ($segStart,$segStop) = @{$$segmentHoA{$k}};
	my $tempOut = File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_tmp".$k.".temp" );
	
	# fork & execute
	my $pid = fork(); 
	if ($pid) { # parent
		print "fork child: $pid\n" if $debug;
		push @children, $pid;
		push @tempFiles, $tempOut;
	} else { # child
		my $value = seqSelector(\%listHash, $seqFile, $seqFileFormat, $segStart, $segStop, $tempOut, $reportType);
		if($sem->op(0, -1, 0)){ 
			print "semaphore get for $tempOut\n" if $debug;
			print $writer "$value\n";
			if($sem->op(0, 1, 0)){ 
				print "semaphore release for $tempOut\n" if $debug;
				# do nothing
			}else{
				print "semaphore release child error\n";
			}
			exit(0);
		}else{
			print "semaphore lock child error\n";
			exit(1);
		}
	}	
}

# wait for each process finished
my $cnt;
foreach my $child (@children) {
	my $line = <$reader>;
	chomp($line);
	
	print "get count $line\n" if $debug;
	
	$cnt += $line;
	
	print "wait child $child\...\n" if $debug;
	waitpid($child, 0);
	print "wait child $child\, DONE\n" if $debug;
	die "some child process exit with error: $?\n" if $?;	
}
print "selected size = $cnt\n"; 
$sem->remove;

# merge outputs
eval{  
	# redirect possible output to STDERR of the command to STDOUT
	open(CPERR, ">&STDERR");
	open(STDERR,">&STDOUT");
	my $cmd;
	if($reportType eq "RAW"){
		$cmd = $splitterInstance->getMergeCommand(\@tempFiles, $outputFile);
	}else{
		$cmd = "cat @tempFiles > $outputFile";
	}
	my $retVal = system $cmd;
	close(STDERR);
	# redirect STDERR back if execution error
	open(STDERR, ">&CPERR");
	die "execution of cat error\n" if $retVal;
	close(CPERR);
};
print "merge error: $@\n" if $@;

# remove tmp directory
rmtree($tmpdir);
my $tries = 0;
while($tries < 10 && -d "$tmpdir") {
	select(undef, undef, undef, 0.75);
	rmtree($tmpdir);
	$tries++;
}
die "cannot remove $tmpdir\n" if (-d "$tmpdir");

#-------------------------------
# subroution 
#-------------------------------
sub seqSelector {
	my ($targetHash, $seqfile, $seqfile_format, $start, $stop, $outfile, $report_type)=@_;
	my $count=0;

	# open
	my $seqInstance = $modules{$seqfile_format}->new($seqfile);
	my $outHandler;
	if($report_type eq "RAW"){
		$outHandler = $seqInstance->createWrite($outfile);
	}else{
		open($outHandler,">","$outfile") or die "fail to create output file: $outfile";
	}
	
	# seek
	$seqInstance->doSeek($start);

	my $printFlag = 0;
	while( $seqInstance->getNextRecord() ){
		last if(defined $stop && $seqInstance->doTell()>$stop);
		
		my $sid;
		if($idBy{sf()} eq "ID"){
			$sid = $seqInstance->getID();
		}elsif($idBy{sf()} eq "SEQ"){
			$sid = $seqInstance->getSEQ();
		}else{
			my $ID = $seqInstance->getID();
			my $SEQ = $seqInstance->getSEQ();
			$sid = eval $progString;
		}
		next if not defined $sid;
		
		my $id = id_replacement($sid,sf);
		my $trimmed = trim_id($id);
		if(exists $$targetHash{$trimmed} 
			&& ((not $mateOnly) || (not exists $$targetHash{$trimmed}{$id}) || (keys %{$$targetHash{$trimmed}})>1)
		){
			$printFlag=1;
		}else{
			$printFlag=0;
		}
	
		# print output
		if($printFlag){
			if($report_type eq "RAW"){
				$seqInstance->writeRAW($outHandler);
			}else{
				my $record;
				if($report_type eq "ID"){
					$record = $seqInstance->getID."\n";
				}elsif($report_type eq "SEQ"){
					$record = $seqInstance->getSEQ."\n";
				}elsif($report_type eq "FASTA"){
					$record = ">";
					$record .= $seqInstance->getID."\n";
					$record .= $seqInstance->getSEQ."\n";
				}
				print $outHandler $record;
			}
			
			$count++;
		}	
	}
	
	# close
	if($report_type eq "RAW"){
		$seqInstance->closeWrite($outHandler);
	}else{
		close($outHandler);
	}
	$seqInstance->closeFile();

	print "selection for $outfile done: $count\n" if $debug;
	
	return $count;
}

sub id_replacement {
	my ($id,$key) = @_;
	$id =~ s/\ /$space{$key}/g if %space && exists $space{$key}; 
	$id =~ s/\//$slash{$key}/g if %slash && exists $slash{$key};
	return $id;	
}

sub trim_id {
	my $id = shift;
	$id = substr($id,0,length($id)-$headerTailTrimLen) if($headerTailTrimLen>0);
	return $id;
}
