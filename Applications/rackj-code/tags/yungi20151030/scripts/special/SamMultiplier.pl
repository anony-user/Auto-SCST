#!/usr/bin/perl -w

my $usage = "Usage: SamMultiplier.pl outFilename pattern [[-P prefix] SAMfile]+\n";

my $outFilename = shift or die $usage;
my $pattern = shift or die $usage;

my %prefixHash=();
my @filenameList=();
while(@ARGV > 0){
    my $option = shift;
    if($option eq "-P"){
        my $prefix = shift or die $usage;
        my $filename = shift or die $usage;
        $prefixHash{$filename}=$prefix;
        push @filenameList,$filename;
        next;
    }else{
        push @filenameList,$option;
    }
}

open(OUTFILE,">$outFilename");
for my $filename (@filenameList){
    open(INFILE,"<$filename");
    
    my $lastSeqID = "";
    my @recordArray = ();
    my $currentSeqID;
    my $prefix = "";
    $prefix = $prefixHash{$filename} if exists $prefixHash{$filename};
    while(<INFILE>) {
        next if (/^@/); # sam header
        $_ = trim($_);

        my @s0 = split;
        $currentSeqID = $s0[0];
        if( $s0[0] ne $lastSeqID ){
            multiply(\@recordArray,$lastSeqID,$prefix) if $lastSeqID ne "";
            @recordArray = ();
            $lastSeqID = $s0[0];
        }
    
        push(@recordArray,\@s0);
    }
    multiply(\@recordArray,$currentSeqID,$prefix);
    close INFILE;
}
close OUTFILE;

sub multiply {
    my $recordArrayRef = shift;
    my $seqID = shift;
    my $prefix = shift;
    
    $seqID =~ /$pattern/;

    if ($1>1) {
        for $i (1 ... $1) {
            for $recordRef (@{$recordArrayRef}){
                ${$recordRef}[0] = $prefix.$seqID."-$i";
                print OUTFILE join("\t",@{$recordRef})."\n";
            }
        }
    }else{
        for $recordRef (@{$recordArrayRef}){
            ${$recordRef}[0] = $prefix.$seqID;
            print OUTFILE join("\t",@{$recordRef})."\n";
        }
    }
}

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}
