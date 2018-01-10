#!/usr/bin/perl -w
use File::Spec;
BEGIN {unshift(@INC,$ENV{'INC'}) if defined $ENV{'INC'};}

my $ID=-1;
my $SamTmp = File::Spec->devnull();
my $BlatTarget;
my $includeUnmapped=0;
my $includeMD=0;
my $showcmd=0;
#$showcmd = $ENV{'ShowCMD'} if defined $ENV{'ShowCMD'};

#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-SamTmp') {
		$SamTmp=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-ID') {
		$ID=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-target') {
		$BlatTarget=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-unmap') {
		$includeUnmapped=1;
		delete @arg_idx[$i,$i];
	}elsif ($ARGV[$i] eq '-md') {
		$includeMD=1;
		delete @arg_idx[$i,$i];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

$usage  = "Usage: MappingBlat.pl [options] <target> <query> <SAMout> <BLAT parameters>\n";
$usage .= "Options:\n";
$usage .= "   -ID <threshold>     identity threshold for filtering SAM output. (default: -1, for no filtering)\n";
$usage .= "   -SamTmp <filename>  temporary SAM filename for filtered queries. One per each read. (default: the null device)\n";
$usage .= "   -target <target>    user-defined target FASTA file, to replace that in the mapping table.\n";
$usage .= "   -unmap              apply this to include unmapped records in the output.\n";
my $Target = shift or die $usage;
my $Query  = shift or die $usage;
my $SamOut = shift or die $usage;

my $ParaLine = join(' ', @ARGV);

die "no -out=type allowed in <BLAT parameters>\n" if $ParaLine=~/^-out/ || $ParaLine=~/\s-out/;

my $tmpdir = $ENV{'ALN_TMP'};
$tmpdir = File::Spec->tmpdir() if !defined $ENV{'ALN_TMP'};
my ($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( $tmpdir, 1 ); #assuming no file

my $Proc_ID = $$;
my $pslout  = File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_BLAT.pslx" );

if(!defined $BlatTarget){
	require("MappingTargetTable.pl");
	$BlatTarget = MappingTargetTable($Target,"blat");
}
my $cmd = "blat -out=pslx $ParaLine $BlatTarget $Query $pslout";
print "$cmd\n" if $showcmd;
system($cmd);

my ($volumeQuery,$directoriesQuery,$queryFile) = File::Spec->splitpath( $Query );

$cmd = "psl2sam.pl ";

if($includeMD){
	$cmd.="-md ";
}

$cmd .= "$BlatTarget $pslout | samGetSEQfast.pl STDIN $Query ";

if($includeUnmapped){
	$cmd.="-unmap ";
}

if ($ID==-1) {
	$cmd.="> $SamOut";
}else{
	$cmd.="| sam_filter.pl -msg [Blat]$queryFile -ID $ID $SamOut $SamTmp";
}
print "$cmd\n" if $showcmd;
system($cmd);
