#!/usr/bin/perl -w
use IPC::Open2;

my $usage = "Usage: SSDAcompare.pl <outputFilename> <fineSplice_1> <geneRPKM_1> <name_1> <fineSplice_2> <geneRPKM_2> <name_2> <rackjJARpath> <PvalTh> [<strandCGFF>]\n";

my $FileO = shift or die $usage;
my $FileA = shift or die $usage;
my $rpkmA = shift or die $usage;
my $nameA = shift or die $usage;
my $FileB = shift or die $usage;
my $rpkmB = shift or die $usage;
my $nameB = shift or die $usage;
my $jarPath = shift or die $usage;
my $pThreshold = shift or die $usage;
my $cgff = "null";

$cgff = $ARGV[0] if @ARGV==1;

die "Existing output file: $FileO\n" if (-e "$FileO");

my @FileNameArr;
push @FileNameArr, $FileA, $rpkmA, $FileB, $rpkmB;

for($i = 0; $i < @FileNameArr; $i++) {
    die "Absent data file: $FileNameArr[$i]\n" if (!-e "$FileNameArr[$i]");
}

my %A_spliceMap;
my %B_spliceMap;
my %A_rpkmMap;
my %B_rpkmMap;
my %combineMap;
my %NovelSplice;
my $Novel = 0;
my %geneStrand;

open(AFILE,"<$FileA");
while (<AFILE>) {
    next if /^#/;
    my @token=split(/\s+/);
    next until $token[1] =~ /(\d+)\([+|-]?\d+\)-(\d+)\([+|-]?\d+\)/;
    # $1."-".$2 -> exonPair
    # $A_spliceMap{gene}{exon pair}{splice event}=reads;
    $A_spliceMap{$token[0]}{$1."-".$2}{$token[1]}=$token[2]+0;
    $combineMap{$token[0]}{$1."-".$2}{$token[1]}=1;
    $NovelSplice{$token[0]}{$token[1]}=1 if $token[3] eq "V";
}
close(AFILE);

open(BFILE,"<$FileB");
while (<BFILE>) {
    next if /^#/;
    my @token=split(/\s+/);
    next until $token[1] =~ /(\d+)\([+|-]?\d+\)-(\d+)\([+|-]?\d+\)/;
    # $B_spliceMap{gene}{exon pair}{splice event}=reads;
    $B_spliceMap{$token[0]}{$1."-".$2}{$token[1]}=$token[2]+0;
    $combineMap{$token[0]}{$1."-".$2}{$token[1]}=1;
    $NovelSplice{$token[0]}{$token[1]}=1 if $token[3] eq "V";
}
close(BFILE);

open(File,"<$rpkmA");
while(<File>) {
    next if /^\#/;
    my @token = split;
    my $uniqRead = $token[2]*(1-$token[4]);
    $A_rpkmMap{$token[0]} = sprintf("%.0f", $uniqRead); #$rpkmMap{GeneID} = UniqRead;
}
close(File);

open(File,"<$rpkmB");
while(<File>) {
    next if /^\#/;
    my @token = split;
    my $uniqRead = $token[2]*(1-$token[4]);
    $B_rpkmMap{$token[0]} = sprintf("%.0f", $uniqRead); #$rpkmMap{GeneID} = UniqRead;
}
close(File);

if ($cgff ne "null") {
    open(CGFFread,"<$cgff");
    while (<CGFFread>) {
        next until /^\>/;
        my @token=split(/\s+/);
        my $gene = substr($token[0],1);
        $geneStrand{$gene}=$token[4];
    }
    close(CGFFread);
}

my @NovelArr = keys %NovelSplice;
$Novel = 1 if @NovelArr > 0;

my $pid = open2( *fisherOUT, *fisherIN,"java -classpath $jarPath statistics.FisherExactTest");

my $DA="";
my @resultSet;
for my $gene (sort {$a cmp $b} (keys %combineMap) ) {
    for my $exonpair (sort exonsort (keys %{$combineMap{$gene}}) ) {
        my @events = sort {$a cmp $b} keys %{$combineMap{$gene}{$exonpair}};
        next if @events < 2;
        for my $x (0..@events-2) {
            for my $y ( $x+1 .. @events-1) {
                my ($As_1,$As_2,$Bs_1,$Bs_2)=(0,0,0,0);
                my $splice1=$events[$x];
                my $splice2=$events[$y];
                
                if (exists $A_spliceMap{$gene}{$exonpair}{$splice1}) {
                    $As_1 = $A_spliceMap{$gene}{$exonpair}{$splice1};
                }
                
                if (exists $A_spliceMap{$gene}{$exonpair}{$splice2}) {
                    $As_2 = $A_spliceMap{$gene}{$exonpair}{$splice2};
                }
                
                if (exists $B_spliceMap{$gene}{$exonpair}{$splice1}) {
                    $Bs_1 = $B_spliceMap{$gene}{$exonpair}{$splice1};
                }
                
                if (exists $B_spliceMap{$gene}{$exonpair}{$splice2}) {
                    $Bs_2 = $B_spliceMap{$gene}{$exonpair}{$splice2};
                }
                print fisherIN "$As_1 $As_2 $Bs_1 $Bs_2\n";
                my $msg = <fisherOUT>;
                chomp($msg);
                my @fTokens=split(/\s+/,$msg);
                
                my @spliceDA;
                if ($cgff ne "null") {
                        my $Donor1=0;
                        my $Acceptor1=0;
                        my $Donor2=0;
                        my $Acceptor2=0;
                    if ($geneStrand{$gene} eq "+") {
                        $splice1 =~ /\d+\(([+|-]?\d+)\)-\d+\(([+|-]?\d+)\)/;
                        $Donor1=$1; $Acceptor1=$2;
                        $splice2 =~ /\d+\(([+|-]?\d+)\)-\d+\(([+|-]?\d+)\)/;
                        $Donor2=$1; $Acceptor2=$2;
                    }elsif ($geneStrand{$gene} eq "-"){
                        $splice1 =~ /\d+\(([+|-]?\d+)\)-\d+\(([+|-]?\d+)\)/;
                        $Donor1=$2; $Acceptor1=$1;
                        $splice2 =~ /\d+\(([+|-]?\d+)\)-\d+\(([+|-]?\d+)\)/;
                        $Donor2=$2; $Acceptor2=$1;
                    }
                    push(@spliceDA,"D") if $Donor1!=$Donor2;
                    push(@spliceDA,"A") if $Acceptor1!=$Acceptor2;
                    $DA = "\t".join("/",@spliceDA);
                }
                
                my @spliceDB=(0,0);
                my ($A_uniqRead,$B_uniqRead,$sub_A,$sub_B) = (0,0,0,0);
                
                $A_uniqRead = $A_rpkmMap{$gene} if (exists $A_rpkmMap{$gene});
                $B_uniqRead = $B_rpkmMap{$gene} if (exists $B_rpkmMap{$gene});
                
                if ($Novel) {
                    if (exists $NovelSplice{$gene}{$splice1}) {
                        $splice1 .= "\t";
                    }else{
                        $splice1 .= "\tV";
                        $spliceDB[0]=1;
                    }
                    if (exists $NovelSplice{$gene}{$splice2}) {
                        $splice2 .= "\t";
                    }else{
                        $splice2 .= "\tV";
                        $spliceDB[1]=1;
                    }
                }
                
                if ($spliceDB[0]+$spliceDB[1]==1) {
                    if ($spliceDB[0]) {
                        ($sub_A,$sub_B)=($As_2,$Bs_2);
                    } elsif ($spliceDB[1]) {
                        ($sub_A,$sub_B)=($As_1,$Bs_1);
                    }
                }else{
                    if ($As_1+$Bs_1<$As_2+$Bs_2) {
                        ($sub_A,$sub_B)=($As_1,$Bs_1);
                    }else{ 
                        ($sub_A,$sub_B)=($As_2,$Bs_2);
                    }
                }
                
                print fisherIN "$A_uniqRead $B_uniqRead $sub_A $sub_B\n";
                $msg = <fisherOUT>;
                chomp($msg);
                my @fTokens_sub=split(/\s+/,$msg);
                
                my $result_msg = "$gene\t$splice1\t$splice2$DA\t$As_1\t$As_2\t$Bs_1\t$Bs_2\t$fTokens[2]";
                $result_msg .= "\t$A_uniqRead\t$B_uniqRead\t$fTokens_sub[2]";
                
                push(@resultSet,["$result_msg","$fTokens[2]"]) if $fTokens[2]<=$pThreshold;
                
            }
        }
    }
}
close(fisherOUT);
close(fisherIN);
open(OFILE, ">$FileO");
my $DA_Head="";
$DA_Head = "Donor/Acceptor Change\t" if $cgff ne "null";
my $novelHead="";
$novelHead="Database\t" if $Novel;
my $Header = "#Gene\tSplice1\t".$novelHead."Splice2\t$novelHead$DA_Head$nameA Splice1\t$nameA Splice2\t$nameB Splice1\t$nameB Splice2\tp-value";
$Header .= "\t$nameA Uniq\t$nameB Uniq\tp-value";
print OFILE "$Header\n";

@resultSet = sort {$a->[1] <=> $b->[1]} @resultSet;
for (@resultSet) {
    my @array= @{$_};
    print OFILE "$array[0]\n";
}
close(OFILE);

sub exonsort {
    my ($a1, $a2) = $a =~ /(\d+)-(\d+)/;
    my ($b1, $b2) = $b =~ /(\d+)-(\d+)/;
    $a1 <=> $b1 or $a2 <=> $b2;
}
