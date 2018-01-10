#!/usr/bin/perl -w

my $debugFile = 0;

$usage   = "Usage: samGetSEQ.pl [-space <X>] [-slash <Y>] <inSAM> <FASTA>+\n";

my $space;
my $slash;

#Retrieve optional parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-space') {
		$space=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
		$i++;
	}elsif ($ARGV[$i] eq '-slash') {
		$slash=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
		$i++;
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

$samfile = shift or die $usage;
die $usage if @ARGV < 1;

sub outDATA {
    $perv_SEQhead = $_[1];
    $perv_SEQ     = $_[2];
    @FASTAs       = @{$_[0]};
    @idArr = @{$_[4]};
    
    for $i ( 0 ... @FASTAs - 1 ) {
        my $isFASTQ = 0;
        
        open(FASTAstream, $FASTAs[$i]) or die("Error: cannot open FASTA file '$FASTAs[$i]'\n");
        while (my $line=<FASTAstream>) {
			$line = trim($line);
			print "." if ($.%1000000)==0 && $debugFile;
			
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
            
            if ($line =~ /^\>/)    #FASTA header ">"
            {
                if (exists ${$_[3]}{$perv_SEQhead}) {
                    foreach my $sline ( @{${$_[3]}{$perv_SEQhead}} ) {
                        my @ss = split( /\t/, trim($sline) );
                        $ss[9] = $perv_SEQ;
                        $ss[9] =~ tr/ATCGatcg/TAGCtagc/ if $ss[1] & 16;
                        $ss[9] = reverse $ss[9] if $ss[1] & 16;
                        $ss[5] =~ tr/H/S/; 
                        $sline = join( "\t", @ss );
                    }
                }

                $perv_SEQhead = substr($line,1);
                $perv_SEQ = "";
            }else{    #FASTA sequence
                $perv_SEQ .= $line;
            }
        }

        # write last SEQ if the record match
        if (exists ${$_[3]}{$perv_SEQhead}) {
            foreach my $sline (@{${$_[3]}{$perv_SEQhead}}) {
                my @ss = split( /\t/, trim($sline) );
                $ss[9] = $perv_SEQ;
                $ss[9] =~ tr/ATCG/TAGC/ if $ss[1] & 16;
                $ss[9] = reverse $ss[9] if $ss[1] & 16;
                $ss[5] =~ tr/H/S/;
                $sline = join( "\t", @ss );
            }
        }
        close FASTAstream;
    }
    
    # output
    foreach my $id (@idArr){
    	foreach my $line (@{${$_[3]}{$id}}){
    		print $line."\n";
    	}
    }
    
}

sub outDATA_hash {
    $perv_SEQhead = $_[1];
    $perv_SEQ     = $_[2];
    @FASTAs       = @{$_[0]};
    
    for $i ( 0 ... @FASTAs - 1 ) {
        open(FASTAstream, $FASTAs[$i]) or die("Error: cannot open FASTA file '$FASTAs[$i]'\n");
        while (<FASTAstream>) {
            if (/^\>/)    #FASTA header ">"
            {
                if (exists ${$_[3]}{$perv_SEQhead}) {
                    foreach my $sline ( @{${$_[3]}{$perv_SEQhead}} ) {
                        my @ss = split( /\t/, trim($sline) );
                        $ss[9] = $perv_SEQ;
                        $ss[9] =~ tr/ATCG/TAGC/ if $ss[1] & 16;
                        $ss[9] = reverse $ss[9] if $ss[1] & 16;
                        $ss[5] =~ tr/H/S/;
                        print join( "\t", @ss ), "\n";
                    }
                }
                delete ${$_[3]}{$perv_SEQhead};

                $perv_SEQhead = trim(substr($_,1));
                $perv_SEQ = "";
            }else{    #FASTA sequence
                $perv_SEQ .= trim($_);
            }
        }

        # write last SEQ if the record match
        if (exists ${$_[3]}{$perv_SEQhead}) {
            foreach my $sline (@{${$_[3]}{$perv_SEQhead}}) {
                my @ss = split( /\t/, trim($sline) );
                $ss[9] = $perv_SEQ;
                $ss[9] =~ tr/ATCG/TAGC/ if $ss[1] & 16;
                $ss[9] = reverse $ss[9] if $ss[1] & 16;
                $ss[5] =~ tr/H/S/;
                print join( "\t", @ss ), "\n";
            }
        }
        delete ${$_[3]}{$perv_SEQhead};
        close FASTAstream;
    }

    # write sam records that no fasta matching
    my @samLines = values(%{$_[3]});
    for $line (@samLines) { print @{$line}; }
}

my $samCount = 0;
my %samHOA;
my @idArray = ();

$samfile = "-" if $samfile eq "STDIN";
open( samFILE, $samfile ) or die("Error: cannot open sam file '$samfile'\n");
while (<samFILE>) {
    if (/^@/)    # sam header
    {
        print;
    }else{       # sam record
        $samCount++;
        my $line = $_;
        my @s    = split;
        push @idArray, $s[0] if not exists $samHOA{$s[0]};
        push @{$samHOA{$s[0]}},$line;
    }

    if ($samCount>= 8000000) #write once when count match and clear sam buffer
    {
        &outDATA(\@ARGV,"NotExist","",\%samHOA,\@idArray );
        $samCount = 0;
        %samHOA = ();
        @idArray = ();
    }
}
close samFILE;

&outDATA(\@ARGV,"NotExist","",\%samHOA,\@idArray ); # write data that not match write count

sub trim {
    my $str = shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}

