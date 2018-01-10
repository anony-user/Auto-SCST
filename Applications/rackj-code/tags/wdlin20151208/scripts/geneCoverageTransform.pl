#!/usr/bin/perl -w
use List::Util qw(sum);
use POSIX qw(ceil);
my $usage   = "Usage: geneCoverageTransform.pl [options] <5UTR/CDS/3UTR length file> <GeneCoverage> <output average depth file> <output filtered gene>\n";
    $usage .= "Options:\n";
    $usage .= "   -cdsLen       CDS region length in final output (default: 100)\n";
    $usage .= "   -cdsMinLen    minimum CDS length for a gene to be included (default: 100)\n";
    $usage .= "   -cdsMinCnt    miniumu sum of CDS region for a gene to be included (default: 1500)\n";
    $usage .= "   -utrMaxLen    maximum UTR length for a gene to be included (default: not defined)\n";

my $cdsLength=100;
my $normalize_value = 1000;
#Filter Threshold
my $cdsLengthTH=100;
my $utrLengthTH;
my $cdsCountTH=1500;
my %filterGene=();

# retrieve options
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-f') {
	        $filterGene{$ARGV[$i+1]}=1;
		delete @arg_idx[$i,$i+1];
	}elsif($ARGV[$i] eq '-cdsLen'){
	        $cdsLength = $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif($ARGV[$i] eq '-cdsMinLen'){
	        $cdsLengthTH = $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif($ARGV[$i] eq '-cdsMinCnt'){
	        $cdsCountTH = $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif($ARGV[$i] eq '-utrMaxLen'){
	        $utrLengthTH = $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

my $lengthFile = shift or die $usage;
my $geneCoveragefile = shift or die $usage;
my $avgFile = shift or die $usage;
my $filteredOut = shift or die $usage;

my %geneRegion;
open(LFILE,"<$lengthFile");
while(<LFILE>){
    next if /^#/;
    chomp;
    my @token=split;
    # 5UTR
    $geneRegion{$token[0]}{'UTR5'}=$token[1];
    # CDS
    $geneRegion{$token[0]}{'CDS'}=$token[2];
    # 3UTR
    $geneRegion{$token[0]}{'UTR3'}=$token[3];
}
close LFILE;

open(AVGF,">$avgFile");
open(FILD,">$filteredOut");

my %norm_genecoverage;
my ($max_5utrLength,$max_3utrLength)=(1,1);
open(GCF,"<$geneCoveragefile");
while(<GCF>) {
    next if /^\#/;
    if (/(\S+)\t\d+\t\d+\t\[(.+?)\]/) {
        my ($gene,$coverage_str) = ($1,$2);
        my @gcAr = split(/\,\s/,$coverage_str);
        my (@cdsAr,@utr5Ar,@utr3Ar);
        if (exists $filterGene{$gene}) { # Filter gene name
            print FILD "$gene\tGene name filter\n";
            next;
        }
        #CDS
        if (exists $geneRegion{$gene}{'CDS'}) {
            my ($top,$bottom) = ($geneRegion{$gene}{'UTR5'},$geneRegion{$gene}{'UTR5'}+$geneRegion{$gene}{'CDS'}-1);
            @cdsAr = @gcAr[$top..$bottom];
            if($bottom-$top+1 < $cdsLengthTH){  # Filter CDS Length
                print FILD "$gene\tCDS length filter\n";
                next;
            }
        }
        if (sum(@cdsAr) < $cdsCountTH ) { # Filter CDS Count
            print FILD "$gene\tCDS Count filter\n";
            next;
        }

        my @newCDSAr = gcTransform(\@cdsAr,$cdsLength);

        my $cdsRatio = $cdsLength/@cdsAr;
        
        #5'UTR
        if (exists $geneRegion{$gene}{'UTR5'}) {
            my ($top,$bottom) = (0,$geneRegion{$gene}{'UTR5'}-1);
            @utr5Ar = @gcAr[$top..$bottom];
            if (defined $utrLengthTH && $bottom-$top+1 > $utrLengthTH ) { # Filter UTR Length
                print FILD "$gene\tUTR Length filter\n";
                next;
            }
        }
            
        my $utr5Length = int(@utr5Ar*$cdsRatio);
        $max_5utrLength = $utr5Length if $utr5Length > $max_5utrLength;
        my @newUTR5Ar = gcTransform(\@utr5Ar,$utr5Length);
        
        #3'UTR
        if (exists $geneRegion{$gene}{'UTR3'}) {
            my ($top,$bottom) = ($geneRegion{$gene}{'UTR5'}+$geneRegion{$gene}{'CDS'},
                $geneRegion{$gene}{'UTR5'}+$geneRegion{$gene}{'CDS'}+$geneRegion{$gene}{'UTR3'}-1);
            @utr3Ar = @gcAr[$top..$bottom];
            if (defined $utrLengthTH && $bottom-$top+1 > $utrLengthTH ) { # Filter UTR Length
                print FILD "$gene\tUTR Length filter\n";
                next;
            }
        } 
        my $utr3Length = int(@utr3Ar*$cdsRatio);
        $max_3utrLength = $utr3Length if $utr3Length > $max_3utrLength;
        my @newUTR3Ar = gcTransform(\@utr3Ar,$utr3Length);
        
        @{$norm_genecoverage{$gene}{'CDS'}}=@newCDSAr;
        @{$norm_genecoverage{$gene}{'UTR5'}}=@newUTR5Ar;
        @{$norm_genecoverage{$gene}{'UTR3'}}=@newUTR3Ar;
    }
}
close(GCF);

#print header
$headerStr = 5 x $max_5utrLength . C x $cdsLength . 3 x $max_3utrLength;
my @header = split("",$headerStr);
print AVGF "Name\t".join("\t",@header)."\n";

my %geneResultAr = ();
# norm for covarage sum
for $gene (keys %norm_genecoverage) {
    my @cdsAr = @{$norm_genecoverage{$gene}{'CDS'}};
    my @utr5Ar = @{$norm_genecoverage{$gene}{'UTR5'}};
    my @utr3Ar = @{$norm_genecoverage{$gene}{'UTR3'}};
    
    my $gcSum = sum(@utr5Ar,@cdsAr,@utr3Ar);
    my $normRatio=0;
    $normRatio = $normalize_value/$gcSum if $gcSum >0;
    my (@norm_cdsAr,@norm_utr5Ar,@norm_utr3Ar);
    
    #CDS
    for $val (@cdsAr) {
        $val = $val*$normRatio;
        push(@norm_cdsAr,$val);
    }
    
    #5'UTR
    for $val (@utr5Ar) {
        $val = $val*$normRatio;
        push(@norm_utr5Ar,$val);
    }
    my $complement_length = $max_5utrLength-@norm_utr5Ar;
    my $zeros="0" x $complement_length;
    unshift(@norm_utr5Ar,split("",$zeros));
    
    #3'UTR
    for $val (@utr3Ar) {
        $val = $val*$normRatio;
        push(@norm_utr3Ar,$val);
    }
    $complement_length = $max_3utrLength-@norm_utr3Ar;
    $zeros="0" x $complement_length;
    push(@norm_utr3Ar,split("",$zeros));
    
    
    #print join("\t",@norm_utr5Ar)."\t".join("\t",@norm_cdsAr)."\t".join("\t",@norm_utr3Ar)."\n";
    push @{$geneResultAr{$gene}}, @norm_utr5Ar,@norm_cdsAr,@norm_utr3Ar;
}

my $arLength = $max_5utrLength+$cdsLength+$max_3utrLength;
my @avgAr;
for $j (0 ... $arLength-1) {
    my $arCount=0;
    my $arSum=0;
    for my $key (keys %geneResultAr){
        if ($geneResultAr{$key}[$j] =~ /\d+/) {
            $arCount++;
            $arSum += $geneResultAr{$key}[$j];
        }
    }
    $avgAr[$j]=$arSum/$arCount;
}

print AVGF "AVG\t".join("\t",@avgAr)."\n";
for my $key (sort {$a cmp $b} keys %geneResultAr){
    print AVGF "$key\t".join("\t",@{$geneResultAr{$key}})."\n";
}

close(AVGF);
close(FILD);

sub gcTransform {
    my @coverageAr=@{$_[0]};
    my $Bin=$_[1];
    
    my @binAr;
    return @binAr if $Bin == 0;
    my $arSize = @coverageAr;
    my $step = $arSize/$Bin;
    for $i (1 ... $Bin) {
        my $bin_val=0;
        my $head_Pos = $arSize*($i-1)/$Bin;
        my $tail_Pos = $arSize*$i/$Bin;
        my $head_idx = ceil($head_Pos);
        $head_idx++ if $head_idx == $head_Pos;
        my $tail_idx = int($tail_Pos);
        $tail_idx-- if $tail_idx == $tail_Pos;
           
        if ($tail_idx >= $head_idx) {
            for $j ($head_idx+1 ... $tail_idx) {
                $bin_val += $coverageAr[$j-1];
            }
        }
        
        if ($tail_Pos>$head_idx) {
            $bin_val += $coverageAr[$head_idx-1]*($head_idx-$head_Pos);
            $bin_val += $coverageAr[$tail_idx]*($tail_Pos-$tail_idx);
        }else{
            $bin_val += $coverageAr[$tail_idx]*($tail_Pos-$head_Pos);
        }
        
        push(@binAr,$bin_val);
    }
    return @binAr;
}

