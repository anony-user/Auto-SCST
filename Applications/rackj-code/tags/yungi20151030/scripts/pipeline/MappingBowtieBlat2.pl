#!/usr/bin/perl -w
use File::Spec;

$usage  = "Usage: MappingBowtieBlat2.pl <target> <query> <SAMout> -bowtie <MappingBowtie.pl options> -blat1 <MappingBlat.pl 1st-pass options> -blat2 <MappingBlat.pl 2nd-pass options>\n";
my $Target = shift or die $usage;
my $Query = shift or die $usage;
my $SamOut = shift or die $usage;
die $usage if @ARGV < 1;

my $tmpdir = $ENV{'ALN_TMP'};
$tmpdir = File::Spec->tmpdir() if !defined $ENV{'ALN_TMP'};
my ($volumeTmp,$directoriesTmp,$filenameTmp) = File::Spec->splitpath( $tmpdir, 1 ); #assuming no file
my $showcmd=0;
#$showcmd = $ENV{'ShowCMD'} if defined $ENV{'ShowCMD'};

my $Proc_ID=$$;
my $SamBowtie   =File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_SamBowtie.sam" );
my $bowtieTmp   =File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_bowtieTmp.sam" );
my $Blat1Tmp    =File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_Blat1Tmp.sam" );
my $Blat1Query  =File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_Blat1.fasta" );
my $Blat2Query  =File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_Blat2.fasta" );
my $BlatSam1pass=File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_BlatSam1pass.sam" );
my $BlatSam2pass=File::Spec->catpath( $volumeTmp, $directoriesTmp, $Proc_ID."_BlatSam2pass.sam" );
my $ParaLine=join(' ',@ARGV);
my ($BowtieParameter,$Blat1Parameter,$Blat2Parameter)=($ParaLine=~m/-bowtie(\s.+)?\s-blat1(\s.+)\s-blat2(\s.+)/);

# check bowtie & blat1 parameter ID is set or not
die "no -SamTmp allowed in <MappingBowtie.pl options>\n" if $BowtieParameter=~/\s-SamTmp/;
$BowtieParameter .= " -ID 0" if $BowtieParameter!~/-ID/;
die "no -SamTmp allowed in <MappingBlat.pl 1st-pass options>\n" if $Blat1Parameter=~/\s-SamTmp/;
$Blat1Parameter .= " -ID 0" if $Blat1Parameter!~/-ID/;

my $cmd="MappingBowtie.pl $Target $Query $SamBowtie -SamTmp $bowtieTmp $BowtieParameter";
print "$cmd\n" if $showcmd;
system($cmd);

$cmd="sam2fas.pl $bowtieTmp $Blat1Query";
print "$cmd\n" if $showcmd;
system($cmd);

$cmd="MappingBlat.pl $Target $Blat1Query $BlatSam1pass -SamTmp $Blat1Tmp $Blat1Parameter ";
print "$cmd\n" if $showcmd;
system($cmd);

$cmd="sam2fas.pl $Blat1Tmp $Blat2Query";
system($cmd);
print "$cmd\n" if $showcmd;

$cmd="MappingBlat.pl $Target $Blat2Query $BlatSam2pass $Blat2Parameter";
system($cmd);
print "$cmd\n" if $showcmd;

$cmd="MergeSam.pl $SamBowtie $BlatSam1pass $BlatSam2pass >$SamOut";
print "$cmd\n" if $showcmd;
system($cmd);

#unlink($bowtieTmp);
#unlink($Blat1Query);
#unlink($SamBowtie);
#unlink($BlatSam);
