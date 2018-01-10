#!/usr/bin/perl -w
use File::Spec;

$usage  = "Usage: MappingBowtieBlat.pl <target> <query> <SAMout> -bowtie <MappingBowtie.pl options> -blat <MappingBlat.pl options>\n";
my $Target = shift or die $usage;
my $Query = shift or die $usage;
my $SamOut = shift or die $usage;
die $usage if @ARGV < 1;

my $tmpdir = $ENV{'ALN_TMP'};
$tmpdir = File::Spec->tmpdir() if !defined $ENV{'ALN_TMP'};
my ($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( $tmpdir, 1 ); #assuming no file
my $showcmd=0;
##$showcmd = $ENV{'ShowCMD'} if defined $ENV{'ShowCMD'};

my $Proc_ID=$$;
my $bowtieTmp=File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_bowtieTmp.sam" );
my $SamBowtie=File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_SamBowtie.sam" );
my $BlatQuery=File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_BlatQuery.fas" );
my $BlatSam  =File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_BlatSam.sam"   );

my $ParaLine=join(' ',@ARGV);
my ($BowtieParameter,$BlatParameter)=($ParaLine=~m/-bowtie(\s.+)?\s-blat(\s.+)/);

# check bowtie parameters
die "no -SamTmp allowed in <MappingBowtie.pl options>\n" if $BowtieParameter=~/\s-SamTmp/;
$BowtieParameter .= " -ID 0" if $BowtieParameter!~/-ID/;

my $cmd="MappingBowtie.pl $Target $Query $SamBowtie -SamTmp $bowtieTmp $BowtieParameter";
print "$cmd\n" if $showcmd;
system($cmd);

$cmd="sam2fas.pl $bowtieTmp $BlatQuery";
print "$cmd\n" if $showcmd;
system($cmd);

$cmd="MappingBlat.pl $Target $BlatQuery $BlatSam $BlatParameter ";
print "$cmd\n" if $showcmd;
system($cmd);

$cmd="MergeSam.pl $SamBowtie $BlatSam >$SamOut";
print "$cmd\n" if $showcmd;
system($cmd);
