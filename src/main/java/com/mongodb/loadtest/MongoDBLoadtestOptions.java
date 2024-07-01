package com.mongodb.loadtest;

import org.apache.commons.cli.*;

//Yes - lots of public values, getters are OTT here.

public class MongoDBLoadtestOptions {

	String connectionDetails = "mongodb://localhost:27017";
	int duration = 5;
	int numThreads = 1;
	int contexts = 500;
	int queryTargettingMaxRatio = 100000;
	String contextFile = null;

	boolean helpOnly = false;
	public boolean skipExhaust = false;
	boolean includeUpdates = true;
	public boolean includeFindAndModify = true;
	public boolean includeFind = true;
	public boolean includeAggregate = true;
	public String dbMerge = null;
	public boolean hints = false;
	
	MongoDBLoadtestOptions(String[] args) throws ParseException
	{
		CommandLineParser parser = new DefaultParser();
		
		Options cliopt;
		cliopt = new Options();
		cliopt.addOption("d","duration", true, "Duration in minutes. Default 5");
		cliopt.addOption("h", "hint", false, "Enable hints, default: disable");
		cliopt.addOption("t", "threads", true, "Number of threads. Default 1");
		cliopt.addOption("u","uri",true,"MongoDB connection details (default 'mongodb://localhost:27017' )");
		cliopt.addOption("c", "ctx", true, "Number of contexts to test. Default: 500");
		cliopt.addOption("i", "ctxId", true, "Context file to include");
		cliopt.addOption("m", "maxRatio", true, "Max allowed Query targeting ratio (Docs scanned vs returned). Default: 100000");
		cliopt.addOption("s", "skipExhaust", false, "When specified the cursors are not exhausted, only initial batch will be extracted.");
		cliopt.addOption("p","updates",true,"Include updates in the test. Default: true");
		cliopt.addOption("f","findAndModify",true,"Include findAndModify in the test. Default: true");
		cliopt.addOption("q","find",true,"Include find in the test. Default: true");
		cliopt.addOption("a","aggregates",true,"Include aggregates in the test. Default: true");
		cliopt.addOption("D","db",true,"Define the db name where all collection will be stored.");

		CommandLine cmd = parser.parse(cliopt, args);

		if (cmd.hasOption("d")){
			duration = Integer.parseInt(cmd.getOptionValue("d"));
		}

		if (cmd.hasOption("p")){
			includeUpdates = Boolean.parseBoolean(cmd.getOptionValue("p"));
		}

		if (cmd.hasOption("q")){
			includeFind = Boolean.parseBoolean(cmd.getOptionValue("q"));
		}
		if (cmd.hasOption("a")){
			includeAggregate = Boolean.parseBoolean(cmd.getOptionValue("a"));
		}

		if (cmd.hasOption("f")){
			includeFindAndModify = Boolean.parseBoolean(cmd.getOptionValue("f"));
		}

		if (cmd.hasOption("h")) {
			hints = true;
		}
		if(cmd.hasOption("help"))
		{
			HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "LoadTestMain", cliopt );
            helpOnly = true;	
		}

		if(cmd.hasOption("s"))
		{
			skipExhaust = true;
		}

		if (cmd.hasOption("i")) {
			contextFile = cmd.getOptionValue("i");
		}

		if (cmd.hasOption("t")){
			numThreads = Integer.parseInt(cmd.getOptionValue("t"));
		}

		if (cmd.hasOption("c")){
			contexts = Integer.parseInt(cmd.getOptionValue("c"));
		}

		if(cmd.hasOption("u"))
		{
			connectionDetails = cmd.getOptionValue("u");
		}
		if (cmd.hasOption("m")) {
			queryTargettingMaxRatio = Integer.parseInt(cmd.getOptionValue("m"));
		}
		if(cliopt.hasOption("D"))
		{
			dbMerge = cmd.getOptionValue("D");
		}
	}
}
