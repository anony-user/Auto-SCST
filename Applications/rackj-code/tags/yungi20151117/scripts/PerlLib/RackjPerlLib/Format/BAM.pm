package RackjPerlLib::Format::BAM;

use warnings;
use strict;
use Bio::DB::Sam;
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
	
	$self->{inBam} = undef;
    $self->{header} = undef;
    $self->{record} = undef;
    
    $self->openFile();
    $self->readHeader();
}

sub openFile {
	my $self = shift;
    my $filename = $self->get_filename;
	$self->{inBam} = Bio::DB::Bam->open("$filename","r");
}

sub closeFile {}

sub readHeader {
	my $self = shift;
	$self->{inBam}->seek(0,0);
	$self->{header} = $self->{inBam}->header; # Bio::DB::Bam::Header
}

sub doSeek {
	my ($self,$startPos) = @_;
	$self->{inBam}->seek($startPos,0);
}

sub doTell {
	my $self = shift;
	my $curr = $self->{inBam}->tell();
	return $curr;
}

sub getID {
	my $self = shift;
	my $id = $self->{record}->query->name; # Bio::DB::Bam::Query
	return $id;
}

sub getSEQ {
	my $self = shift;
	my $dna = $self->{record}->query->dna; 
	my $strand = $self->{record}->query->strand;
	
	if($strand == -1){
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
	my $align = $self->{inBam}->read1; # Bio::DB::Bam::Alignment 
	return 0 if not defined $align;
	$self->{record} = $align;
	return 1;
}


sub getSplitPosition {
	my $self = shift;
	my $splitNum = shift;
	my $noSplitSameID = 1;
	$noSplitSameID = shift if(defined $_[0]);
	
	my $bam = $self->{inBam};
	$self->readHeader();
	my $startPos = $bam->tell();
	my @pointers=(); 
	my @ids=();
	my $num=1;
	while (my $align = $bam->read1) {
		if($num%10000==0){
			push @pointers, $bam->tell();
			push @ids, $align->query->name;
		}
		$num++;
	}
	my $endPos = $bam->tell(); # this endPos is different from the end position of the real last record
	push @pointers, $endPos;

	$splitNum = @pointers if $splitNum>@pointers;
	$splitNum = 1 if $splitNum<1;
	my $share = int (@pointers)/$splitNum;
	$share++ if ((@pointers)%$splitNum)>0; 
	
	my %sliceIdHash=();
	my $c = $share;
	while( $c < @pointers ){
		my $ptr = $pointers[$c-1];
		my $id = $ids[$c-1];
		$sliceIdHash{$id} = $ptr; # replace previous if the same ID
		$c += $share;
	}
	
	my @arr;
	if($noSplitSameID){
		my %slicePosHash = reverse %sliceIdHash;
		my @newPos=();
		foreach my $slice ( sort {$a<=>$b} keys %slicePosHash ){
			my $slice_id = $slicePosHash{$slice};
			$bam->seek($slice,0);
			while(my $align = $bam->read1){
				if($align->query->name ne $slice_id){
					push @newPos, $slice;
					last;
				}
				$slice = $bam->tell();
			}
		}
		@arr = ($startPos, @newPos, $endPos);
	}else{
		my @slicePos = sort {$a<=>$b} values %sliceIdHash;
		@arr = ($startPos, @slicePos, $endPos);
	}
	
	my %posHoA;
	for(my $k=1; $k<@arr; $k++){
		push @{$posHoA{$k}}, ($arr[$k-1], $arr[$k]);
	}
	return \%posHoA;
}

sub getMergeCommand {
	my ($self, $filenameArr, $outputFilename) = @_;
	if(@{$filenameArr}>1){
		return "samtools cat -o $outputFilename @{$filenameArr}";
	}else{
		return "cp @{$filenameArr} $outputFilename";
	}
}

sub createWrite {
	my ($self, $filename) = @_;
	my $bam = Bio::DB::Bam->open("$filename","w");
	$bam->header_write($self->{header});
	return $bam;
}

sub writeRAW {
	my $self = shift;
	my $bam = shift;
	my $data;
	if(defined $_[0]){
		$data = $_[0];
	}else{
		$data = $self->{record};
	}
	$bam->write1($data);
}

sub closeWrite {}


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