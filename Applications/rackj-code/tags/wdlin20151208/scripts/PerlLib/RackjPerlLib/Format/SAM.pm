package RackjPerlLib::Format::SAM;
use warnings;
use strict;
use File::stat;
use Carp;


sub new {
	my $class = shift;
	my $arg = shift or croak "filename argument required";
    my $self = bless {}, $class;
    $self->_initialize($arg);
    return $self;
}

sub _initialize {
	my ($self, $filename) = @_;
	$self->set_filename($filename);
	
	$self->{inFh} = $self->openFile();
    $self->{header} = $self->readHeader();
    $self->{record} = undef;
}

sub openFile {
    my $self = shift;
    my $filename = $self->get_filename;
    open(my $fh,"<","$filename") or croak "fail to open file: $filename";
    return $fh;
}

sub closeFile {
    my $self = shift;
    close($self->{inFh});
}

sub readHeader {
	my $self = shift;
	my $curr=0;
	my @headers;
	my $fh = $self->{inFh};
	seek($fh,0,0);
	while( <$fh> ){
		if($_ !~ /^@/){
			seek($self->{inFh}, $curr, 0);
			last;
		}
		$curr = tell();
		push @headers, $_;
	}
	return \@headers;
}

sub doSeek {
	my ($self, $startPos) = @_;
	seek($self->{inFh}, $startPos, 0);
}

sub doTell {
	my $self = shift;
	my $curr = tell($self->{inFh});
	return $curr;
}

sub getID {
	my $self = shift;
	my $record = $self->{record};
	$record =~ s/(^\s+|\s+$)//g;
	my @token = split(/\t/, $record);
	return $token[0];
}

sub getSEQ {
	my $self = shift;
	my $record = $self->{record};
	$record =~ s/(^\s+|\s+$)//g;
	my @token = split(/\t/, $record);
	my $dna = $token[9];
	
	if($token[1] & 16){
		my $seq = reverse($dna);
		$seq =~ tr/ACGTacgt/TGCAtgca/;
		return $seq;
	}
	
	return $dna;
}

sub getRAW {
	my $self = shift;
	return $self->{record};
}

sub getNextRecord {
	my $self = shift;
	my $fh = $self->{inFh};
	my $line = <$fh>;
	
	return 0 if(not defined $line);
	$self->{record} = $line;
	return 1;
}

sub getSplitPosition {
	my $self = shift;
	my $splitNum = shift;
	my $noSplitSameID = 1;
	$noSplitSameID = shift if(defined $_[0]);
	
	my $filename = $self->get_filename;
	my $size = stat($filename)->size;
	my $testFh = $self->{inFh};
	my $head = $self->readHeader();
	my $fromPos = tell($testFh);
	$splitNum = 1 if $splitNum<1;
	my $share = int(($size-$fromPos)/$splitNum);
	
	my %sliceIdHash=();
	my $c=1;
	while($c < $splitNum){
		my $Pos = $fromPos + ($c*$share);
		seek($testFh, $Pos, 0);
		my $curr = <$testFh>;
		my $next = <$testFh>;
		if(defined $next){
			$Pos = tell($testFh);
			$next =~ s/(^\s+|\s+$)//g;
			my @tokens = split(/\t/, $next);
			$sliceIdHash{$tokens[0]} = $Pos; # later slice positions with the same ID will be replaced
		}
		$c++;
	}
	
	my @arr;
	if($noSplitSameID){
		my %slicePosHash = reverse %sliceIdHash;
		my @newPos=();
		foreach my $slice ( sort {$a<=>$b} keys %slicePosHash ){
			my $slice_id = $slicePosHash{$slice};
			seek($testFh, $slice, 0);
			while(<$testFh>){
				my @token = split(/\t/, $_);
				if($token[0] ne $slice_id){
					push @newPos, $slice;
					last;
				}
				$slice = tell($testFh);
			}
		}
		@arr = ($fromPos, @newPos, $size);
	}else{
		my @slicePos = sort {$a<=>$b} values %sliceIdHash;
		@arr = ($fromPos, @slicePos, $size);
	}
	
	my %posHoA;
	for(my $k=1; $k<@arr; $k++){
		push @{$posHoA{$k}}, ($arr[$k-1], $arr[$k]);
	}
	return \%posHoA;
}

sub getMergeCommand {
	my ($self, $filenameArr, $outputFilename) = @_;
	return "MergeSam.pl @{$filenameArr} > $outputFilename";
}

sub createWrite {
	my ($self, $filename) = @_;
	open(my $fh,">","$filename") or croak "fail to create output file: $filename";
	for(@{$self->{header}}){
		print $fh "$_";
	}
	return $fh;
}

sub writeRAW {
	my $self = shift;
	my $fhandle = shift;
	my $data;
	if(defined $_[0]){
		$data = shift;
	}else{
		$data = $self->getRAW();
	}
	print $fhandle "$data";
}

sub closeWrite {
	my ($self, $fhandle) = @_;
	close($fhandle);
}


# getter / setter
sub get_filename {
	my $self = shift;
	return $self->{attributes}{filename};
}

sub set_filename {
	my ($self, $name) = @_;
	$self->{attributes}{filename} = $name;
}


1;