
1. To let Bio::DB::Sam access large BAM files:
	a. build samtools (up to 0.1.19) with -fPIC in CFLAGS
	b. make sure BioPerl is installed
	c. download Bio::DB::Sam (Bio-SamTools-1.39 and 1.41 tested)
	d. download Math::Int64 (Math-Int64-0.30 and 0.34 tested)
	e. use Bio-SamTools-1.39.typemap.patch to patch Bio-SamTools-1.39/typemap (or Bio-SamTools-1.41/typemap)
	f. use Bio-SamTools-1.39.lib.Bio.DB.Sam.xs.patch to patch Bio-SamTools-1.39/lib/Bio/DB/Sam.xs (or 1.41 correspondences)
	g. copy Math-Int64-0.30/c_api_client/perl_math_int64.* to Bio-SamTools-1.39/c_bin/
	h. copy Math-Int64-0.30/ppport.h to Bio-SamTools-1.39/c_bin/
	i. build Bio::DB::Sam as described in README (need samtools 0.1.10+ and BioPerl installed)
	j. build Math::Int64 as described in README
	k. make sure that both Math::Int64 and Bio::DB::Sam are installed in the system

