#!/usr/bin/perl
use File::Spec;
BEGIN {unshift(@INC,$ENV{'INC'}) if defined $ENV{'INC'};}

my $ID=-1;
my $SamTmp = File::Spec->devnull();
my $GmapTarget;
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
		$GmapTarget=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

$usage = "Usage: MappingGmap.pl [options] <target> <query> <SAMout> <GMAP parameters>\n";
$usage .= "Options:\n";
$usage .= "   -ID <threshold>     identity threshold for filtering SAM output. (default: -1, for no filtering)\n";
$usage .= "   -SamTmp <filename>  temporary SAM filename for filtered queries. One per each read. (default: the null device)\n";
$usage .= "   -target <target>    user-defined target index prefix, to replace that in the mapping table.\n";
my $Target = shift or die $usage;
my $Query  = shift or die $usage;
my $SamOut = shift or die $usage;

my $ParaLine = join(' ', @ARGV);

if(!defined $GmapTarget){
	require("MappingTargetTable.pl");
	$GmapTarget = MappingTargetTable($Target,"gmap");
}
my ($gmapVolume,$gmapDirectories,$gmapTarget) = File::Spec->splitpath( $GmapTarget );

my $cmd ="gmap -O -f samse --no-sam-headers ";

if(length($gmapDirectories)>0){
	$cmd .= "-D $gmapDirectories ";
}

$cmd .= "-d $gmapTarget $ParaLine $Query ";

$cmd .= "| MD2XM.pl ";

my ($volumeQuery,$directoriesQuery,$queryFile) = File::Spec->splitpath( $Query );

if ($ID==-1) {
	$cmd.="> $SamOut";
}else{
	$cmd.="| sam_filter.pl -msg [GMAP]$queryFile -ID $ID $SamOut $SamTmp";
}
print "$cmd\n" if $showcmd;

# redirect possible output to STDERR to NULL
my $nullpath = File::Spec->devnull();
open(CPERR, ">&STDERR");
open(STDERR,">$nullpath");

system($cmd);

open(STDERR, ">&CPERR");
