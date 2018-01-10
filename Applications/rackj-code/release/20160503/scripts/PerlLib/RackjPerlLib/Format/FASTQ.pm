package RackjPerlLib::Format::FASTQ;
use warnings;
use strict;
use File::stat;
use Carp;


sub new {
	my $class = shift;
	my $arg = shift or croak "filename argument required";
	my $self = {};
    bless $self, $class;
    $self->_initialize($arg);
    return $self;
}

sub _initialize {
	my ($self, $filename) = @_;
	$self->set_filename($filename);
	
	$self->{inFh} = $self->openFile();
    $self->{recordLines} = [];
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

sub readHeader {}

sub doSeek {
	my ($self,$startPos) = @_;
	seek($self->{inFh}, $startPos, 0);
}

sub doTell {
	my $self = shift;
	my $curr = tell($self->{inFh});
	return $curr;
}

sub getID {
	my $self = shift;
	my $line = $self->{recordLines}[0];
	$line =~ s/(^\s+|\s+$)//g;
	my $id = substr($line,1);
	return $id;
}

sub getSEQ {
	my $self = shift;
	my $seq = $self->{recordLines}[1];
	$seq =~ s/(^\s+|\s+$)//g;
	return $seq;
}

sub getRAW {
	my $self = shift;
	return $self->{recordLines};
}

sub getNextRecord {
	my $self = shift;
	my $fh = $self->{inFh};
	@{$self->{recordLines}}=(); # reset
	while( <$fh> ){	
		push @{$self->{recordLines}}, $_;
		if( @{$self->{recordLines}}==4 ){
			if( length($self->{recordLines}[1]) != length($self->{recordLines}[3]) ){
				@{$self->{recordLines}} = ();
				# TODO: output some error message
				warn "invalid FastQ format near line number $.\n";
			}
			last;	
		}
	}
	return 0 if @{$self->{recordLines}}==0;
	return 1;
}

sub getSplitPosition {
	my ($self,$splitNum) = @_;
	my $filename = $self->get_filename;
	my $testFh = $self->{inFh};
	
	my $filesize = stat($filename)->size;
	my $share = int($filesize/$splitNum);
	
	my %posHoA;
	my $fromPos = 0;
	my $i=1;
	while($i<$splitNum){
		my $toPos = $i*$share;
		seek($testFh, $toPos, 0);
		my $line = <$testFh>;
		my $lastLineStart = tell($testFh);
		while(not eof $testFh){
			# get a first line starting with @
			while($line=<$testFh>){
				last if $line=~/^@/;	# break if meet possible sequence header
				$lastLineStart = tell($testFh);
			}
			# break if there is no such line
			last if eof $testFh;

			# get next line
			$line=<$testFh>;
			last if eof $testFh; # break if there is no next line
			
			$line =~ s/(^\s+|\s+$)//g;
			last if($line=~/^[ACGTNacgtn]+$/);
		}
		$lastLineStart = tell($testFh) if(eof $testFh); # must reach eof
		$toPos = $lastLineStart;
		if($toPos>$fromPos && $toPos<$filesize){
			push @{$posHoA{$i}}, $fromPos, $toPos; # [ startPos, stopPos ]
			$fromPos = $toPos;
		}
		
		$i++;
	}
	push @{$posHoA{$i}}, $fromPos, $filesize;
	close($testFh);
	
	return \%posHoA;
}

sub getMergeCommand {
	my ($self, $filenameArr, $outputFilename) = @_;
	return "cat @{$filenameArr} > $outputFilename";
}

sub createWrite {
	my ($self, $filename) = @_;
	open(my $fh,">","$filename") or croak "fail to create output file: $filename";
	return $fh;
}

sub writeRAW {
	my $self = shift;
	my $fhandle = shift;
	my @data;
	if(defined $_[0]){
		push @data, $_[0];
	}else{
		@data = @{$self->getRAW()};
	}
	print $fhandle "$_" for(@data);
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