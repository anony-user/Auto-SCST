#!/usr/bin/perl -w
use Bio::DB::Fasta;

my $fasFile = shift;

my $db = Bio::DB::Fasta->new($fasFile,'-reindex',1);
system "samtools faidx $fasFile";
